package com.ssafy.b109.aivo.llm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.interview.dto.InterviewAnswerSubmitRequest;
import com.ssafy.b109.aivo.interview.dto.InterviewContentEvaluationResponse;
import com.ssafy.b109.aivo.interview.dto.InterviewReportResponse;
import com.ssafy.b109.aivo.interview.dto.NonverbalSummaryResponse;
import com.ssafy.b109.aivo.interview.dto.QuestionEvaluationResponse;
import com.ssafy.b109.aivo.interview.entity.Interview;
import com.ssafy.b109.aivo.interview.entity.InterviewQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReportGenerator {

    private static final int MAX_ANSWER_CONTEXT_LENGTH = 12_000;
    private static final int MAX_INTERVIEW_BEST_ANSWER_CONTEXT_LENGTH = 8_000;
    private static final int REPORT_GENERATION_ATTEMPTS = 2;
    private static final int REPORT_MAX_COMPLETION_TOKENS = 8_000;
    private static final String NOT_SELECTED = "선택하지 않음";
    private static final String REPORT_JSON_SCHEMA = """
            {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "overallScore": {
                  "type": "integer",
                  "description": "Overall interview score from 0 to 100."
                },
                "contentEvaluation": {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "relevanceScore": {
                      "type": "integer",
                      "description": "Question relevance score from 0 to 100."
                    },
                    "structureScore": {
                      "type": "integer",
                      "description": "Answer structure score from 0 to 100."
                    },
                    "clarityScore": {
                      "type": "integer",
                      "description": "Answer clarity score from 0 to 100."
                    },
                    "deliveryScore": {
                      "type": "integer",
                      "description": "Delivery score from 0 to 100."
                    },
                    "feedback": {
                      "type": "string",
                      "description": "Overall content feedback."
                    }
                  },
                  "required": [
                    "relevanceScore",
                    "structureScore",
                    "clarityScore",
                    "deliveryScore",
                    "feedback"
                  ]
                },
                "questionEvaluations": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "questionId": {
                        "type": "integer",
                        "description": "Question ID provided in the prompt."
                      },
                      "question": {
                        "type": "string"
                      },
                      "answer": {
                        "type": "string"
                      },
                      "score": {
                        "type": "integer",
                        "description": "Question answer score from 0 to 100."
                      },
                      "feedback": {
                        "type": "string"
                      },
                      "improvement": {
                        "type": "string"
                      },
                      "problem": {
                        "type": "string"
                      },
                      "issueLabel": {
                        "type": "string"
                      },
                      "evidence": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "type": {
                              "type": "string",
                              "enum": [
                                "strength",
                                "weakness"
                              ]
                            },
                            "text": {
                              "type": "string"
                            },
                            "startIndex": {
                              "type": [
                                "integer",
                                "null"
                              ]
                            },
                            "endIndex": {
                              "type": [
                                "integer",
                                "null"
                              ]
                            },
                            "reason": {
                              "type": "string"
                            }
                          },
                          "required": [
                            "type",
                            "text",
                            "startIndex",
                            "endIndex",
                            "reason"
                          ]
                        }
                      }
                    },
                    "required": [
                      "questionId",
                      "question",
                      "answer",
                      "score",
                      "feedback",
                      "improvement",
                      "problem",
                      "issueLabel",
                      "evidence"
                    ]
                  }
                },
                "strengths": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "improvements": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "detailedFeedback": {
                  "type": "string"
                }
              },
              "required": [
                "overallScore",
                "contentEvaluation",
                "questionEvaluations",
                "strengths",
                "improvements",
                "detailedFeedback"
              ]
            }
            """;
    private static final OpenAiChatModel.ResponseFormat REPORT_RESPONSE_FORMAT =
            OpenAiChatModel.ResponseFormat.builder()
                    .jsonSchema(REPORT_JSON_SCHEMA)
                    .build();

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public InterviewReportResponse generate(
            Interview interview,
            Long practiceId,
            String companyName,
            String occupationName,
            String jobName,
            List<String> companyBestContents,
            List<String> interviewBestAnswerContents,
            String companyResearchContext,
            NonverbalSummaryResponse nonverbalSummary,
            List<InterviewQuestion> questions,
            List<InterviewAnswerSubmitRequest> answers
    ) {
        String prompt = buildPrompt(
                interview,
                companyName,
                occupationName,
                jobName,
                companyBestContents,
                interviewBestAnswerContents,
                companyResearchContext,
                nonverbalSummary,
                questions,
                answers
        );

        Exception lastException = null;
        for (int attempt = 1; attempt <= REPORT_GENERATION_ATTEMPTS; attempt++) {
            String content = requestReportContent(prompt, attempt);
            try {
                LlmReportResponse parsed = parseReport(content);
                return parsed.toResponse(interview.getId(), practiceId, interview.getTitle(), nonverbalSummary);
            } catch (Exception exception) {
                lastException = exception;
                if (attempt < REPORT_GENERATION_ATTEMPTS) {
                    log.debug(
                            "Retrying LLM interview report generation after unparsable response. attempt={}/{}, responseLength={}, reason={}",
                            attempt,
                            REPORT_GENERATION_ATTEMPTS,
                            contentLength(content),
                            exception.getMessage()
                    );
                }
            }
        }

        log.warn(
                "Failed to parse LLM interview report after {} attempts: {}",
                REPORT_GENERATION_ATTEMPTS,
                lastException == null ? "unknown parse failure" : lastException.getMessage()
        );
        throw new CustomException(ErrorCode.REPORT_GENERATION_FAILED);
    }

    private String requestReportContent(String prompt, int attempt) {
        return chatClientBuilder.build()
                .prompt(promptWithRetryInstruction(prompt, attempt))
                .options(reportOptions())
                .call()
                .content();
    }

    private OpenAiChatOptions.Builder reportOptions() {
        return OpenAiChatOptions.builder()
                .maxCompletionTokens(REPORT_MAX_COMPLETION_TOKENS)
                .responseFormat(REPORT_RESPONSE_FORMAT);
    }

    private String promptWithRetryInstruction(String prompt, int attempt) {
        if (attempt <= 1) {
            return prompt;
        }

        return prompt + """

                이전 응답은 완성된 JSON 객체가 아니어서 파싱에 실패했다.
                이번에는 중간에 끊기지 않은 완전한 JSON 객체 하나만 반환해라.
                """;
    }

    private String buildPrompt(
            Interview interview,
            String companyName,
            String occupationName,
            String jobName,
            List<String> companyBestContents,
            List<String> interviewBestAnswerContents,
            String companyResearchContext,
            NonverbalSummaryResponse nonverbalSummary,
            List<InterviewQuestion> questions,
            List<InterviewAnswerSubmitRequest> answers
    ) {
        return """
                너는 채용 면접관이자 커리어 코치다.
                아래 모의 면접 데이터를 바탕으로 질문별 답변 평가와 종합 리포트를 생성해라.
                반드시 JSON 객체 하나만 반환하고, 마크다운 코드블록/설명/주석은 넣지 마라.

                평가 기준:
                - 모든 점수는 0~100 정수로 산정한다.
                - 질문별 feedback과 improvement는 해당 질문/답변 내용에 근거해 작성한다.
                - 선택하지 않은 회사/직군/직무/기업 인재상은 평가 근거로 사용하지 않는다.
                - 값이 있는 회사/직군/직무/인재상은 질문 맥락과 답변 적합성 평가에 반영한다.
                - 기업별 기출 질문/답변이 있으면, 해당 기업에서 기대하는 답변 관점과 역량을 참고해 평가한다.
                - 최신 기업 리서치 context는 질문의 회사·직무 맥락을 이해하는 참고 자료로만 사용한다.
                - 리서치 context에 없는 기업 사실을 추측하지 말고, 답변 평가를 기업 정보 하나만으로 단정하지 않는다.
                - 단, 기출 모범 답변과 다른 표현을 사용했다는 이유만으로 감점하지 말고, 핵심 역량과 논리 적합성을 기준으로 평가한다.
                - 비언어 통계는 deliveryScore와 detailedFeedback에 반영한다.
                - 답변이 비어 있으면 낮은 점수와 구체적인 보완 방향을 제시한다.
                - 각 questionEvaluation에는 evidence 배열을 포함한다.
                - evidence는 feedback, improvement, problem 판단의 근거가 되는 답변 원문 일부다.
                - evidence[].type은 반드시 "strength" 또는 "weakness" 중 하나만 사용한다.
                - evidence[].text는 반드시 answer에 실제로 존재하는 연속 문자열을 그대로 복사한다. 요약하거나 바꾸지 않는다.
                - evidence[].reason은 해당 구간이 왜 판단 근거인지 한 문장으로 작성한다.
                - evidence[].startIndex와 evidence[].endIndex는 null로 반환한다. 인덱스는 서버가 계산한다.
                - questionEvaluations는 생성된/제출된 질문 수만큼만 작성한다.
                - 각 feedback과 improvement는 2문장 이내로 작성한다.
                - evidence는 질문당 최대 2개만 작성하고, evidence[].text는 80자 이내로 작성한다.
                - strengths와 improvements는 각각 최대 3개만 작성한다.
                - detailedFeedback은 600자 이내로 작성한다.

                반환 JSON schema:
                사용자 노출 필드 규칙:
                - strengths, improvements, detailedFeedback, contentEvaluation.feedback, questionEvaluations[].feedback/improvement/problem/evidence[].reason은 사용자에게 직접 노출되는 문장이다.
                - 사용자 노출 문장에는 questionId, 내부 ID, 질문 번호, 순번을 절대 포함하지 마라. "623번 답변", "질문 624", "questionId=624" 같은 표현도 금지한다.
                - 여러 답변을 구분해야 하면 숫자나 ID 대신 질문의 주제, 질문 내용, 또는 "해당 답변"처럼 자연어로 표현하라.
                - JSON의 questionId 필드는 매핑을 위해 questionEvaluations 객체 안에서만 사용하고, 그 값을 다른 사용자 노출 필드의 문장에 복사하지 마라.
                - 최종 JSON을 반환하기 전에 사용자 노출 필드 전체를 검사하여 숫자+번, questionId, ID, 내부 식별자 표현을 제거하라.

                {
                  "overallScore": 0,
                  "contentEvaluation": {
                    "relevanceScore": 0,
                    "structureScore": 0,
                    "clarityScore": 0,
                    "deliveryScore": 0,
                    "feedback": "전체 답변 내용 평가"
                  },
                  "questionEvaluations": [
                    {
                      "questionId": 1,
                      "question": "질문",
                      "answer": "답변",
                      "score": 0,
                      "feedback": "답변의 강점과 문제점",
                      "improvement": "다음 답변에서 바로 고칠 구체적 개선안",
                      "problem": "가장 큰 문제 한 문장",
                      "issueLabel": "짧은 이슈 라벨",
                      "evidence": [
                        {
                          "type": "strength",
                          "text": "answer 원문에서 그대로 복사한 연속 문자열",
                          "startIndex": null,
                          "endIndex": null,
                          "reason": "이 구간이 판단 근거인 이유"
                        }
                      ]
                    }
                  ],
                  "strengths": ["강점"],
                  "improvements": ["개선점"],
                  "detailedFeedback": "종합 피드백"
                }

                면접 제목: %s
                회사: %s
                직군: %s
                직무: %s
                경력: %s
                면접관 스타일: %s

                기업 인재상:
                %s

                기업별 기출 질문/답변:
                %s

                최신 기업 리서치 context:
                %s

                비언어/음성 누적 통계:
                - 분석 청크 수: %d
                - 추임새 총 횟수: %d
                - 침묵 감지 횟수: %d
                - 말더듬 감지 횟수: %d
                - 평균 WPM: %d

                생성된 면접 질문:
                %s

                제출된 질문/답변:
                %s
                """.formatted(
                valueOrNotSelected(interview.getTitle()),
                valueOrNotSelected(companyName),
                valueOrNotSelected(occupationName),
                valueOrNotSelected(jobName),
                valueOrNotSelected(interview.getWorkExperience()),
                interview.getInterviewer() == null ? NOT_SELECTED : valueOrNotSelected(interview.getInterviewer().getName()),
                listOrNotSelected(companyBestContents),
                limitedListOrNotSelected(interviewBestAnswerContents, MAX_INTERVIEW_BEST_ANSWER_CONTEXT_LENGTH),
                valueOrNotSelected(companyResearchContext),
                nonverbalSummary.analyzedChunks(),
                nonverbalSummary.totalFillerCount(),
                nonverbalSummary.silenceCount(),
                nonverbalSummary.stutterCount(),
                nonverbalSummary.averageWpm(),
                buildQuestionContext(questions),
                buildAnswerContext(answers)
        );
    }

    private int contentLength(String content) {
        return content == null ? 0 : content.length();
    }

    private String buildQuestionContext(List<InterviewQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return "생성된 질문 없음";
        }

        return questions.stream()
                .map(question -> "- questionId=%d, question=%s".formatted(
                        question.getId(),
                        valueOrNotSelected(question.getQuestion())
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("생성된 질문 없음");
    }

    private String buildAnswerContext(List<InterviewAnswerSubmitRequest> answers) {
        if (answers == null || answers.isEmpty()) {
            return "제출된 답변 없음";
        }

        String context = answers.stream()
                .map(answer -> "- questionId=%s%nQ. %s%nA. %s".formatted(
                        answer.questionId() == null ? "null" : answer.questionId(),
                        valueOrNotSelected(answer.question()),
                        valueOrNotSelected(answer.answer())
                ))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("제출된 답변 없음");

        return context.length() > MAX_ANSWER_CONTEXT_LENGTH
                ? context.substring(0, MAX_ANSWER_CONTEXT_LENGTH)
                : context;
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(ErrorCode.REPORT_GENERATION_FAILED);
        }

        String trimmed = stripMarkdownFence(content.trim());

        int start = trimmed.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("LLM interview report response does not contain a JSON object");
        }

        int end = findMatchingObjectEnd(trimmed, start);
        if (end < 0) {
            throw new IllegalArgumentException("LLM interview report JSON object is incomplete");
        }
        return trimmed.substring(start, end + 1);
    }

    private String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }
        return content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    /**
     * JSON 열고 닫힘 매칭 검사
     * @param content
     * @param start
     * @return
     */
    private int findMatchingObjectEnd(String content, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }

            if (character == '"') {
                inString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    private LlmReportResponse parseReport(String content) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(extractJson(content));
        if (rootNode instanceof ObjectNode rootObject) {
            normalizeReportJson(rootObject);
        }
        return objectMapper.treeToValue(rootNode, LlmReportResponse.class);
    }

    private void normalizeReportJson(ObjectNode rootObject) {
        JsonNode contentEvaluationNode = rootObject.get("contentEvaluation");
        if (!(contentEvaluationNode instanceof ObjectNode contentEvaluationObject)) {
            return;
        }

        moveNestedFieldToRoot(contentEvaluationObject, rootObject, "questionEvaluations");
        moveNestedFieldToRoot(contentEvaluationObject, rootObject, "strengths");
        moveNestedFieldToRoot(contentEvaluationObject, rootObject, "improvements");
        moveNestedFieldToRoot(contentEvaluationObject, rootObject, "detailedFeedback");

        contentEvaluationObject.retain(
                "relevanceScore",
                "structureScore",
                "clarityScore",
                "deliveryScore",
                "feedback"
        );
    }

    private void moveNestedFieldToRoot(ObjectNode source, ObjectNode target, String fieldName) {
        JsonNode value = source.get(fieldName);
        if (value != null && !value.isNull() && !target.has(fieldName)) {
            target.set(fieldName, value);
        }
        source.remove(fieldName);
    }

    private String valueOrNotSelected(String value) {
        return value == null || value.isBlank() ? NOT_SELECTED : value;
    }

    private String listOrNotSelected(List<String> values) {
        if (values == null || values.isEmpty()) {
            return NOT_SELECTED;
        }
        return String.join("\n\n", values);
    }

    private String limitedListOrNotSelected(List<String> values, int maxLength) {
        String context = listOrNotSelected(values);
        return context.length() > maxLength ? context.substring(0, maxLength) : context;
    }

    private record LlmReportResponse(
            Integer overallScore,
            InterviewContentEvaluationResponse contentEvaluation,
            List<QuestionEvaluationResponse> questionEvaluations,
            List<String> strengths,
            List<String> improvements,
            String detailedFeedback
    ) {

        private InterviewReportResponse toResponse(
                Long interviewId,
                Long practiceId,
                String title,
                NonverbalSummaryResponse nonverbalSummary
        ) {
            return new InterviewReportResponse(
                    interviewId,
                    practiceId,
                    valueOrEmpty(title),
                    null,
                    null,
                    clamp(overallScore),
                    null,
                    List.of(),
                    null,
                    null,
                    nonverbalSummary,
                    contentEvaluation == null
                            ? new InterviewContentEvaluationResponse(0, 0, 0, 0, "")
                            : contentEvaluation,
                    questionEvaluations == null ? List.of() : questionEvaluations,
                    questionEvaluations == null ? List.of() : questionEvaluations,
                    null,
                    null,
                    null,
                    strengths == null ? List.of() : strengths,
                    improvements == null ? List.of() : improvements,
                    detailedFeedback == null ? "" : detailedFeedback
            );
        }

        private int clamp(Integer score) {
            if (score == null) {
                return 0;
            }
            return Math.max(0, Math.min(100, score));
        }

        private String valueOrEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
