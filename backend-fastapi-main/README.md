# aivo FastAPI AI Backend

FastAPI 기반의 음성 분석 서비스 코드입니다. 업로드된 오디오를 분석 가능한 형식으로 정규화하고, STT·필러·침묵·반복 분석 결과를 Spring Boot 애플리케이션과 연결합니다.

## 코드 둘러보기

| 경로 | 내용 |
| --- | --- |
| [`app/domains/audio_analysis/`](app/domains/audio_analysis/) | 오디오 업로드 API, 응답 스키마, WAV 정규화와 분석 호출 |
| [`app/domains/stt/`](app/domains/stt/) | 전사 API와 세그먼트 응답 모델 |
| [`app/messaging/`](app/messaging/) | 분석 작업·결과 메시지 계약과 라우팅 |
| [`app/workers/`](app/workers/) | 메시지 기반 분석 작업자 |
| [`models/filer/src/`](models/filer/src/) | STT 설정, 필러 보완, VAD·음향 특성, 발화 이벤트·점수 산출 |

## AI 핵심 코드

1. [`presentation_coaching_transcription.py`](models/filer/src/presentation_coaching_transcription.py): 단어 타임스탬프를 포함한 한국어 발표 전사 프로필
2. [`presentation_coaching_filler_rescue.py`](models/filer/src/presentation_coaching_filler_rescue.py): 8초 창 재전사와 0.3초 중복 병합
3. [`presentation_coaching_events.py`](models/filer/src/presentation_coaching_events.py): 필러·반복·말 늘임·침묵 이벤트 탐지

## 포트폴리오 공개 범위

환경 변수, 클라우드·메시지 브로커 연결 설정, 모델 가중치와 사용자 음성 데이터는 포함하지 않았습니다. 이 폴더는 구현 검토용 스냅샷이며, 운영 환경을 그대로 재현하는 실행 레포가 아닙니다.

모델 선정 근거와 측정 결과는 루트 [`README.md`](../README.md)에서 확인할 수 있습니다.
