# 공개 코드 스냅샷

이 디렉터리는 aivo의 전체 운영 소스를 공개하지 않고, 프로젝트에서 구현한 AI 음성 분석 흐름을 검토할 수 있도록 선별한 코드입니다.

| 영역 | 포함한 구현 | 확인할 파일 |
| --- | --- | --- |
| STT 설정 | 발표 상황에 맞춘 한국어 디코딩 프로필, 단어 시각 정보 | [`ai-stt-core/presentation_coaching_transcription.py`](ai-stt-core/presentation_coaching_transcription.py) |
| 필러 보완 | 전체 전사 뒤 8초 창 재전사, 0.3초 이내 중복 병합 | [`ai-stt-core/presentation_coaching_filler_rescue.py`](ai-stt-core/presentation_coaching_filler_rescue.py) |
| 발화 이벤트 | 필러·반복·말 늘임·침묵을 시간 구간과 근거로 분류 | [`ai-stt-core/presentation_coaching_events.py`](ai-stt-core/presentation_coaching_events.py) |
| 음성 특성 | VAD, RMS 연속성, 피치 기반의 음향 근거 산출 | [`ai-stt-core/presentation_coaching_audio_features.py`](ai-stt-core/presentation_coaching_audio_features.py), [`ai-stt-core/presentation_coaching_vad.py`](ai-stt-core/presentation_coaching_vad.py) |
| 점수·피드백 | 발화 전달력 지표와 코칭 이벤트를 결과로 조합 | [`ai-stt-core/presentation_coaching_delivery_metrics.py`](ai-stt-core/presentation_coaching_delivery_metrics.py), [`ai-stt-core/presentation_coaching_scoring.py`](ai-stt-core/presentation_coaching_scoring.py) |
| 서비스 연결 | 업로드 오디오 정규화, 비동기 분석 호출, API 응답 계약 | [`fastapi-audio-analysis/`](fastapi-audio-analysis/) |

## 코드 읽는 순서

1. `presentation_coaching_transcription.py`에서 단어 단위 타임스탬프를 포함한 전사 옵션을 확인합니다.
2. `presentation_coaching_filler_rescue.py`에서 짧은 필러를 보완 전사하고 중복을 제거하는 흐름을 확인합니다.
3. `presentation_coaching_events.py`에서 ASR·VAD·음향 특성을 함께 사용해 코칭 이벤트를 만드는 기준을 확인합니다.
4. `fastapi-audio-analysis/`에서 해당 분석이 업로드 API와 응답 모델에 연결되는 방식을 확인합니다.

## 공개 범위

이 코드는 포트폴리오 검토를 위한 **읽기 전용 스냅샷**입니다. 모델 가중치, 사용자 음성/영상, 실행 환경 변수, 배포 설정, 클라우드·DB·메시징 연결 정보와 원본 Git 이력은 의도적으로 포함하지 않았습니다. 따라서 이 디렉터리만으로 운영 환경을 재현하는 목적은 아닙니다.

실제 모델 선정 근거와 측정 결과는 루트 [`README.md`](../README.md)의 `AI 음성 분석 구현` 및 `모델 탐색과 최종 선택`에서 확인할 수 있습니다.
