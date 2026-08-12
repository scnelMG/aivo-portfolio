# aivo Frontend

Vue 3·Vite 기반의 aivo 웹 클라이언트 코드입니다. 발표 연습과 AI 면접 연습의 설정·녹화·분석·리포트 화면을 구현하며, Pinia와 Vue Router로 화면 상태와 흐름을 관리합니다.

## 코드 둘러보기

| 경로 | 내용 |
| --- | --- |
| [`src/views/`](src/views/) | 발표·면접·연습 기록·마이페이지의 화면 단위 Vue SFC |
| [`src/components/`](src/components/) | 공통 UI, 카메라 제어, 발표 리포트 컴포넌트 |
| [`src/composables/`](src/composables/) | 녹화·마이크·MediaPipe·실시간 발화 분석 브라우저 로직 |
| [`src/api/`](src/api/) | Spring Boot API 요청, 응답 정규화, 요청 DTO 조합 |
| [`src/stores/`](src/stores/) | Pinia 기반 연습·면접·발표 상태와 API orchestration |
| [`src/services/`](src/services/) | PCM WAV 캡처와 발표 시각 분석 연동 |

## 포트폴리오 공개 범위

이 폴더는 코드 리뷰를 위한 공개 스냅샷입니다. 운영 API 주소, MediaPipe 모델 주소 등 환경별 설정과 내부 테스트·개발 문서는 포함하지 않았습니다. 따라서 이 저장소만으로 배포 환경을 재현하는 목적은 아닙니다.

서비스 흐름과 실제 기능 화면은 루트 [`README.md`](../README.md)에서, 전체 공개 범위는 [`SOURCE_SNAPSHOT.md`](../SOURCE_SNAPSHOT.md)에서 확인할 수 있습니다.
