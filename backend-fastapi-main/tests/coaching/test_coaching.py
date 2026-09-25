"""CPU regression checks; scripted inputs are not accuracy measurements."""
from types import SimpleNamespace
import wave

import pytest

from presentation_coaching_audio import SpeechSpan, silences_from_voiced_spans
from presentation_coaching_evaluation import summarize_review_metrics
from presentation_coaching_filler_rescue import (
    merge_rescued_fillers,
    transcribe_with_filler_rescue,
)
from presentation_coaching_types import CoachingEvent, EventKind, ReviewerVerdict, WordTiming


def test_rescue_preserves_primary_words_and_adds_only_missing_fillers():
    primary = (WordTiming("발표", 1, 1.5), WordTiming("음", 2, 2.2))
    rescued = (
        WordTiming("다른문장", 0, 0.5),
        WordTiming("어", 0.5, 0.7),
        WordTiming("음,", 2.1, 2.3),
        WordTiming("어", 0.6, 0.8),
        WordTiming("음", 4, 4.2),
    )
    assert merge_rescued_fillers(primary, rescued) == (
        rescued[1], *primary, rescued[4]
    )


def test_overlapping_rescue_is_not_counted_twice():
    primary = (WordTiming("음", 1, 2),)
    assert merge_rescued_fillers(primary, (WordTiming("음", 1.5, 2.1),)) == primary


def test_rescue_window_offsets_and_tail_do_not_duplicate_events(tmp_path):
    audio = tmp_path / "silence.wav"
    with wave.open(str(audio), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(16000)
        wav.writeframes(bytes(17 * 16000 * 2))
    calls = []

    class ScriptedTranscriber:
        def transcribe(self, samples, **options):
            calls.append((samples, options))
            # Primary + windows starting at 0, 8, 9 seconds.
            text, start = [("발표", 2.0), ("어", 1.0), ("음", 1.0), ("음", 0.1)][len(calls) - 1]
            word = SimpleNamespace(word=text, start=start, end=start + 0.2)
            return (SimpleNamespace(words=[word]),), None

    result = transcribe_with_filler_rescue(
        ScriptedTranscriber(), audio, {"language": "ko", "word_timestamps": True}
    )
    assert [(word.text, word.start_seconds) for word in result] == [
        ("어", 1.0), ("발표", 2.0), ("음", 9.0)
    ]
    assert [len(samples) for samples, _ in calls[1:]] == [128000] * 3
    assert all(options == {"language": "ko", "word_timestamps": True} for _, options in calls)


def test_pause_threshold_excludes_short_gaps():
    spans = (SpeechSpan(0, 1), SpeechSpan(1.5, 2), SpeechSpan(3, 4))
    assert silences_from_voiced_spans(spans, 1.0) == (SpeechSpan(2, 3),)
    assert silences_from_voiced_spans((), 1.0) == ()


def test_review_metrics_do_not_invent_recall_from_detected_events():
    events = tuple(CoachingEvent(str(i), EventKind.FILLER, i, i + 0.2, "어", {}, "", "high") for i in range(3))
    result = summarize_review_metrics(events, {
        "0": ReviewerVerdict.CORRECT, "1": ReviewerVerdict.INCORRECT
    })
    assert (result.reviewed_count, result.unreviewed_count) == (2, 1)
    assert result.precision == pytest.approx(0.5)
    assert result.false_coaching_rate == pytest.approx(0.5)
    assert result.recall is None and result.f1 is None
    unreviewed = summarize_review_metrics(events, {})
    assert unreviewed.precision is None and unreviewed.false_coaching_rate is None
