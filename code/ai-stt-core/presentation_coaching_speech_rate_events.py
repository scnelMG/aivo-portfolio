"""Utterance-level speech-rate measurements and non-scoring change events."""

from dataclasses import dataclass
from hashlib import sha256
from typing import Final

from presentation_coaching_audio import SpeechSpan, overlap_seconds
from presentation_coaching_events import FILLER_CANDIDATES, TOKEN_EDGE_PUNCTUATION
from presentation_coaching_types import CoachingEvent, EventKind, WordTiming


SENTENCE_GAP_SECONDS: Final = 0.75
MINIMUM_SEGMENT_VOICED_SECONDS: Final = 1.5
MINIMUM_SEGMENT_CONTENT_SYLLABLES: Final = 8
MINIMUM_RATE_CHANGE_SYLLABLES_PER_SECOND: Final = 1.0
MINIMUM_RATE_CHANGE_RATIO: Final = 0.25
SENTENCE_END_PUNCTUATION: Final = ".!?\u3002\uff01\uff1f"


@dataclass(frozen=True, slots=True)
class SpeechRateSegment:
    """One punctuation- or silence-delimited utterance measured with VAD time."""

    start_seconds: float
    end_seconds: float
    sentence: str
    content_syllable_count: int
    voiced_seconds: float
    syllables_per_second: float | None
    eligible_for_change_event: bool


@dataclass(frozen=True, slots=True)
class SpeechRateAnalysis:
    """All utterance rates and deviations from the aggregate articulation rate."""

    segments: tuple[SpeechRateSegment, ...]
    overall_articulation_rate_syllables_per_second: float | None
    events: tuple[CoachingEvent, ...]


def analyze_speech_rate_events(
    words: tuple[WordTiming, ...],
    voiced_spans: tuple[SpeechSpan, ...],
    *,
    sentence_gap_seconds: float = SENTENCE_GAP_SECONDS,
) -> SpeechRateAnalysis:
    """Measure utterance rates and emit non-scoring deviations from the total rate."""
    if sentence_gap_seconds < 0.0:
        raise ValueError("sentence_gap_seconds must be non-negative")
    segments = _segments(words, voiced_spans, sentence_gap_seconds)
    overall_articulation_rate = _overall_articulation_rate(segments)
    return SpeechRateAnalysis(
        segments,
        overall_articulation_rate,
        _change_events(segments, overall_articulation_rate),
    )


def _segments(
    words: tuple[WordTiming, ...],
    voiced_spans: tuple[SpeechSpan, ...],
    sentence_gap_seconds: float,
) -> tuple[SpeechRateSegment, ...]:
    groups: list[tuple[WordTiming, ...]] = []
    current: list[WordTiming] = []
    previous_end = 0.0
    for word in words:
        if current and word.start_seconds - previous_end >= sentence_gap_seconds:
            groups.append(tuple(current))
            current = []
        current.append(word)
        previous_end = word.end_seconds
        if word.text.rstrip().endswith(tuple(SENTENCE_END_PUNCTUATION)):
            groups.append(tuple(current))
            current = []
    if current:
        groups.append(tuple(current))
    return tuple(_segment(group, voiced_spans) for group in groups)


def _segment(
    words: tuple[WordTiming, ...], voiced_spans: tuple[SpeechSpan, ...]
) -> SpeechRateSegment:
    start_seconds = words[0].start_seconds
    end_seconds = words[-1].end_seconds
    span = SpeechSpan(start_seconds, end_seconds)
    content_syllable_count = sum(
        _hangul_syllable_count(word.text)
        for word in words
        if _normalized_token(word.text) not in FILLER_CANDIDATES
    )
    voiced_seconds = sum(overlap_seconds(span, voiced_span) for voiced_span in voiced_spans)
    syllables_per_second = (
        content_syllable_count / voiced_seconds if voiced_seconds > 0.0 else None
    )
    eligible = (
        syllables_per_second is not None
        and voiced_seconds >= MINIMUM_SEGMENT_VOICED_SECONDS
        and content_syllable_count >= MINIMUM_SEGMENT_CONTENT_SYLLABLES
    )
    return SpeechRateSegment(
        start_seconds,
        end_seconds,
        " ".join(word.text for word in words),
        content_syllable_count,
        voiced_seconds,
        syllables_per_second,
        eligible,
    )


def _overall_articulation_rate(
    segments: tuple[SpeechRateSegment, ...],
) -> float | None:
    measured_segments = tuple(
        segment for segment in segments if segment.syllables_per_second is not None
    )
    voiced_seconds = sum(segment.voiced_seconds for segment in measured_segments)
    if voiced_seconds <= 0.0:
        return None
    return sum(segment.content_syllable_count for segment in measured_segments) / voiced_seconds


def _change_events(
    segments: tuple[SpeechRateSegment, ...],
    overall_articulation_rate: float | None,
) -> tuple[CoachingEvent, ...]:
    if overall_articulation_rate is None:
        return ()
    eligible_segments = tuple(
        segment for segment in segments if segment.eligible_for_change_event
    )
    return tuple(
        _event(segment, overall_articulation_rate)
        for segment in eligible_segments
        if _has_material_change(segment, overall_articulation_rate)
    )


def _has_material_change(
    segment: SpeechRateSegment, overall_articulation_rate: float
) -> bool:
    if segment.syllables_per_second is None or overall_articulation_rate <= 0.0:
        return False
    change = abs(segment.syllables_per_second - overall_articulation_rate)
    return (
        change >= MINIMUM_RATE_CHANGE_SYLLABLES_PER_SECOND
        and change / overall_articulation_rate >= MINIMUM_RATE_CHANGE_RATIO
    )


def _event(
    segment: SpeechRateSegment, overall_articulation_rate: float
) -> CoachingEvent:
    assert segment.syllables_per_second is not None
    direction = (
        "faster"
        if segment.syllables_per_second > overall_articulation_rate
        else "slower"
    )
    identity = (
        f"{direction}|{segment.start_seconds:.3f}|{segment.end_seconds:.3f}|"
        f"{segment.syllables_per_second:.3f}|{overall_articulation_rate:.3f}"
    )
    return CoachingEvent(
        f"speech-rate-{sha256(identity.encode()).hexdigest()[:12]}",
        EventKind.SPEECH_RATE,
        segment.start_seconds,
        segment.end_seconds,
        segment.sentence,
        {
            "rate_direction": direction,
            "syllables_per_second": segment.syllables_per_second,
            "baseline_syllables_per_second": overall_articulation_rate,
            "change_syllables_per_second": (
                segment.syllables_per_second - overall_articulation_rate
            ),
            "content_syllable_count": float(segment.content_syllable_count),
            "voiced_seconds": segment.voiced_seconds,
            "scoring_reflected": 0.0,
        },
        _message(direction, segment, overall_articulation_rate),
        "medium",
    )


def _message(
    direction: str,
    segment: SpeechRateSegment,
    overall_articulation_rate: float,
) -> str:
    if direction == "faster":
        return (
            f"전체 조음 속도 {overall_articulation_rate:.1f}음절/초보다 "
            f"이 구간이 {segment.syllables_per_second:.1f}음절/초로 빠릅니다."
        )
    return (
        f"전체 조음 속도 {overall_articulation_rate:.1f}음절/초보다 "
        f"이 구간이 {segment.syllables_per_second:.1f}음절/초로 느립니다."
    )


def _normalized_token(text: str) -> str:
    return text.strip().strip(TOKEN_EDGE_PUNCTUATION)


def _hangul_syllable_count(text: str) -> int:
    return sum("\uac00" <= character <= "\ud7a3" for character in text)
