# aivo Spring Backend

Spring Boot 기반의 aivo 애플리케이션 서버 코드입니다. 인증, 발표·면접 연습 기록, 리포트, 파일 처리와 AI 분석 결과 연동을 담당합니다.

## 코드 둘러보기

| 경로 | 내용 |
| --- | --- |
| [`src/main/java/com/ssafy/b109/aivo/presentation/`](src/main/java/com/ssafy/b109/aivo/presentation/) | 발표 자료·연습·슬라이드·리포트 도메인 |
| [`src/main/java/com/ssafy/b109/aivo/interview/`](src/main/java/com/ssafy/b109/aivo/interview/) | AI 면접 진행, 질문·답변, 분석 결과·리포트 도메인 |
| [`src/main/java/com/ssafy/b109/aivo/practice/`](src/main/java/com/ssafy/b109/aivo/practice/) | 연습 폴더·기록·점수 추이 도메인 |
| [`src/main/java/com/ssafy/b109/aivo/llm/`](src/main/java/com/ssafy/b109/aivo/llm/) | 질문 및 피드백 생성 로직 |
| [`src/main/java/com/ssafy/b109/aivo/rabbitmq/`](src/main/java/com/ssafy/b109/aivo/rabbitmq/) | AI 분석 요청·결과 메시지 처리 |

## 포트폴리오 공개 범위

DB, 메시지 브로커, 클라우드 스토리지, 인증 비밀값 및 배포 설정은 공개하지 않았습니다. 이 폴더는 실제 구현을 읽는 용도의 코드 스냅샷이며, 운영 환경을 그대로 실행하기 위한 배포 패키지가 아닙니다.

전체 구조와 AI 코드 위치는 [`SOURCE_SNAPSHOT.md`](../SOURCE_SNAPSHOT.md)에서 확인할 수 있습니다.
