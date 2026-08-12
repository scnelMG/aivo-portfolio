# aivo 공개 소스 스냅샷

이 저장소는 포트폴리오용 프로젝트 소개와 함께, 실제 aivo 구현의 코드 구조를 검토할 수 있도록 구성했습니다. 폴더 이름과 계층은 원본 프로젝트를 따릅니다.

| 폴더 | 내용 |
| --- | --- |
| [`frontend-vue-main/`](frontend-vue-main/) | Vue 3·Vite 기반 화면, 발표·면접 연습 UI, MediaPipe 및 Web Audio 연동 코드 |
| [`backend-spring-develop/`](backend-spring-develop/) | Spring Boot 기반 인증·연습 기록·리포트·면접 도메인 코드 |
| [`backend-fastapi-main/`](backend-fastapi-main/) | FastAPI 기반 음성 분석 API·작업자·메시징 계약과 STT 연동 코드 |
| [`backend-fastapi-main/models/filer/src/`](backend-fastapi-main/models/filer/src/) | 박민규가 구현한 STT 설정, 필러 보완, VAD·음향 특성, 발화 이벤트·점수 산출 코드 |

## AI 코드 읽는 순서

1. [`presentation_coaching_transcription.py`](backend-fastapi-main/models/filer/src/presentation_coaching_transcription.py): 발표 상황용 한국어 전사 프로필과 단어 타임스탬프 설정
2. [`presentation_coaching_filler_rescue.py`](backend-fastapi-main/models/filer/src/presentation_coaching_filler_rescue.py): 8초 창 재전사와 0.3초 중복 병합으로 짧은 필러 보완
3. [`presentation_coaching_events.py`](backend-fastapi-main/models/filer/src/presentation_coaching_events.py): 필러·반복·말 늘임·침묵을 시간 구간과 근거로 분류
4. [`app/domains/audio_analysis/`](backend-fastapi-main/app/domains/audio_analysis/): 업로드된 오디오를 정규화하고 분석 결과를 API 응답으로 연결

## 의도적으로 제외한 항목

운영 환경을 그대로 복제하거나 비밀값을 노출하지 않기 위해 다음 항목은 포함하지 않았습니다.

- 환경 변수 파일과 런타임 설정값
- DB·메시지 브로커·클라우드 스토리지의 연결 설정 및 배포 구성
- 사용자 음성/영상, DB 덤프, 모델 가중치, 캐시·컴파일 산출물
- 내부 작업 계획·운영 문서와 원본 Git 이력

따라서 이 공개 스냅샷은 **코드 리뷰와 포트폴리오 열람**을 위한 것이며, 운영 서비스를 그대로 실행하기 위한 배포 레포가 아닙니다.
