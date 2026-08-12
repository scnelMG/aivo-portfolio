from dataclasses import dataclass
from importlib.util import find_spec
from pathlib import Path
from typing import Protocol
import os
import time

import av
from presentation_coaching_audio import (
    load_wav_at_target_rate,
    measure_word_audio_from_analysis,
    silences_from_voiced_spans,
)
from presentation_coaching_events import (
    PAUSE_ANALYSIS_MINIMUM_SECONDS,
    EventDetectionDiagnostics,
    analyze_events,
)
from presentation_coaching_filler_rescue import transcribe_with_filler_rescue
from presentation_coaching_model_cache import local_model_path as _local_model_path
from presentation_coaching_model_memory import release_cuda_memory as _release_cuda_memory
from presentation_coaching_resources import ResourcePeaks, ResourceSampler, gpu_name
from presentation_coaching_speech_rate_events import (
    SENTENCE_GAP_SECONDS,
    SpeechRateAnalysis,
    analyze_speech_rate_events,
)
from presentation_coaching_transcription import PRESENTATION_DISFLUENCY_PROFILE
from presentation_coaching_types import ModelRun, RunMetrics, WordTiming
from presentation_coaching_vad import vad_analysis


class _DllDirectoryHandle(Protocol):
    def close(self) -> None: ...


_DLL_DIRECTORY_HANDLES: list[_DllDirectoryHandle] = []
_REGISTERED_DLL_DIRECTORIES: list[Path] = []


@dataclass(frozen=True, slots=True)
class EmptyKoreanTranscriptionError(RuntimeError):
    model_key: str

    def __str__(self) -> str:
        return f"{self.model_key}: transcription returned no Korean words"


@dataclass(frozen=True, slots=True)
class CandidateSpec:
    key: str
    label: str
    model_id: str
    language: str
    compute_type: str
    device: str


@dataclass(frozen=True, slots=True)
class CandidateAvailability:
    available: bool
    reason: str
    local_model_path: Path | None


@dataclass(frozen=True, slots=True)
class BenchmarkedRun:
    model_run: ModelRun
    total_seconds: float
    resources: ResourcePeaks
    audio_path: Path
    event_detection_diagnostics: EventDetectionDiagnostics | None = None
    speech_rate_analysis: SpeechRateAnalysis | None = None


def available_candidates() -> dict[str, CandidateSpec]:
    spec = CandidateSpec(
        key="faster-whisper-large-v3-turbo",
        label="faster-whisper large-v3-turbo",
        model_id="mobiuslabsgmbh/faster-whisper-large-v3-turbo",
        language="ko",
        compute_type="float16",
        device="cuda",
    )
    return {spec.key: spec}


def inspect_candidate(
    spec: CandidateSpec, *, allow_model_downloads: bool = False,
) -> CandidateAvailability:
    if find_spec("faster_whisper") is None:
        return CandidateAvailability(False, "faster-whisper is not installed", None)

    local_path = _local_model_path(spec.model_id)
    if allow_model_downloads and local_path is None and not Path(spec.model_id).is_dir():
        try:
            from huggingface_hub import snapshot_download

            local_path = Path(snapshot_download(spec.model_id, local_files_only=False))
        except Exception as exc:  # noqa: BLE001
            return CandidateAvailability(
                False,
                f"model download failed: {type(exc).__name__}: {exc}",
                None,
            )
    if local_path is None:
        return CandidateAvailability(
            False,
            "The faster-whisper model is not in the local Hugging Face cache.",
            None,
        )
    return CandidateAvailability(True, "local model is available", local_path)


def configure_local_cuda() -> tuple[Path, ...]:
    cuda_path = Path(
        os.environ.get(
            "CUDA_PATH",
            r"C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.8",
        )
    )
    site_packages = Path(__file__).resolve().parent / ".venv" / "Lib" / "site-packages"
    candidates = (
        cuda_path / "bin",
        site_packages / "nvidia" / "cudnn" / "bin",
        site_packages / "torch" / "lib",
        site_packages / "ctranslate2",
    )
    available = tuple(path for path in candidates if path.is_dir())
    new_paths = tuple(path for path in available if path not in _REGISTERED_DLL_DIRECTORIES)
    if new_paths:
        os.environ["PATH"] = os.pathsep.join(
            (*map(str, new_paths), os.environ.get("PATH", ""))
        )
        _REGISTERED_DLL_DIRECTORIES.extend(new_paths)
        if hasattr(os, "add_dll_directory"):
            _DLL_DIRECTORY_HANDLES.extend(
                os.add_dll_directory(str(path)) for path in new_paths
            )
    return available


def run_candidate(spec: CandidateSpec, audio_path: Path) -> ModelRun:
    return benchmark_candidate(spec, audio_path).model_run


def benchmark_candidate(
    spec: CandidateSpec,
    audio_path: Path,
    *,
    allow_model_downloads: bool = False,
    sentence_gap_seconds: float = SENTENCE_GAP_SECONDS,
) -> BenchmarkedRun:
    started = time.perf_counter()
    sampler = ResourceSampler()
    sampler.start()
    load_seconds = 0.0
    transcription_seconds = 0.0
    analysis_seconds = 0.0
    words: tuple[WordTiming, ...] = ()
    events = ()
    event_detection_diagnostics: EventDetectionDiagnostics | None = None
    speech_rate_analysis: SpeechRateAnalysis | None = None
    error: str | None = None
    model = None
    try:
        configure_local_cuda()
        availability = inspect_candidate(spec, allow_model_downloads=allow_model_downloads)
        if not audio_path.is_file():
            raise FileNotFoundError(f"audio file was not found: {audio_path}")
        if not availability.available or availability.local_model_path is None:
            raise RuntimeError(availability.reason)

        load_started = time.perf_counter()
        model = _load_model(spec, availability.local_model_path)
        load_seconds = time.perf_counter() - load_started

        transcription_started = time.perf_counter()
        words = _transcribe(model, audio_path)
        transcription_seconds = time.perf_counter() - transcription_started
        if not words:
            raise EmptyKoreanTranscriptionError(spec.key)

        analysis_started = time.perf_counter()
        samples = load_wav_at_target_rate(audio_path)
        vad_result = vad_analysis(samples)
        voiced_spans = vad_result.voiced_spans
        silences = silences_from_voiced_spans(voiced_spans, PAUSE_ANALYSIS_MINIMUM_SECONDS)
        event_analysis = analyze_events(
            words,
            silences,
            lambda span: measure_word_audio_from_analysis(
                samples,
                vad_result,
                span,
            ),
        )
        speech_rate_analysis = analyze_speech_rate_events(
            words,
            voiced_spans,
            sentence_gap_seconds=sentence_gap_seconds,
        )
        events = tuple(
            sorted(
                (*event_analysis.events, *speech_rate_analysis.events),
                key=lambda event: (event.start_seconds, event.end_seconds, event.event_id),
            )
        )
        event_detection_diagnostics = event_analysis.diagnostics
        analysis_seconds = time.perf_counter() - analysis_started
    except Exception as exc:  # noqa: BLE001
        error = f"{type(exc).__name__}: {exc}"
    finally:
        del model
        _release_cuda_memory()
        resources = sampler.stop()

    duration = _audio_duration(audio_path)
    realtime_factor = transcription_seconds / duration if duration > 0.0 else 0.0
    metrics = RunMetrics(
        load_seconds,
        transcription_seconds,
        analysis_seconds,
        realtime_factor,
        resources.max_vram_mib,
        resources.average_gpu_percent,
        resources.max_process_ram_mib,
    )
    return BenchmarkedRun(
        ModelRun(
            spec.key,
            spec.compute_type,
            gpu_name(),
            words,
            events,
            metrics,
            error,
            PRESENTATION_DISFLUENCY_PROFILE.profile_id,
        ),
        time.perf_counter() - started,
        resources,
        audio_path,
        event_detection_diagnostics,
        speech_rate_analysis,
    )


def _load_model(spec: CandidateSpec, local_path: Path):
    from faster_whisper import WhisperModel

    return WhisperModel(
        str(local_path),
        device=spec.device,
        compute_type=spec.compute_type,
        local_files_only=True,
    )


def _transcribe(model, audio_path: Path) -> tuple[WordTiming, ...]:
    return transcribe_with_filler_rescue(
        model,
        audio_path,
        PRESENTATION_DISFLUENCY_PROFILE.to_transcribe_kwargs(),
    )


def _audio_duration(audio_path: Path) -> float:
    try:
        with av.open(str(audio_path)) as container:
            stream = container.streams.audio[0]
            if stream.duration is not None:
                return float(stream.duration * stream.time_base)
            if container.duration is not None:
                return float(container.duration / av.time_base)
    except (FileNotFoundError, av.FFmpegError, IndexError, ZeroDivisionError):
        return 0.0
    return 0.0
