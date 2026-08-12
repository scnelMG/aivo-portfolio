package com.ssafy.b109.aivo.llm.service;

import com.ssafy.b109.aivo.interview.dto.InterviewStartRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewQuestionGenerator {

    private static final String NOT_SELECTED = "선택하지 않음";
    private static final int MAX_INTERVIEW_BEST_ANSWER_CONTEXT_LENGTH = 8_000;

    private final ChatClient.Builder chatClientBuilder;

    public List<String> generate(
            InterviewStartRequest request,
            String companyName,
            String occupationName,
            String jobName,
            String interviewerName,
            List<String> companyBestContents,
            List<String> interviewBestAnswerContents,
            String companyResearchContext,
            String portfolioContext,
            String resumeContext
    ) {
        String prompt = """
                한국어 모의 면접 질문 10개만 생성해.
                출력은 번호 없이 질문만 줄바꿈으로 작성해.
                아래 정보 중 '선택하지 않음'인 항목은 억지로 반영하지 말고, 선택된 항목만 자연스럽게 반영해.
                기업 인재상이 있으면 질문의 평가 기준과 상황 질문에 반영해.
                기업별 기출 질문/답변이 있으면 해당 기업에서 자주 검증하는 역량과 질문 패턴을 반영해.
                단, 기출 질문을 그대로 복사하지 말고 유사한 의도와 난이도의 새 질문으로 변형해.

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

                포트폴리오 요약:
                %s

                이력서/자소서 본문:
                %s
                """.formatted(
                valueOrNotSelected(companyName),
                valueOrNotSelected(occupationName),
                valueOrNotSelected(jobName),
                valueOrNotSelected(request.workExperience()),
                valueOrNotSelected(interviewerName),
                listOrNotSelected(companyBestContents),
                limitedListOrNotSelected(interviewBestAnswerContents, MAX_INTERVIEW_BEST_ANSWER_CONTEXT_LENGTH),
                valueOrNotSelected(companyResearchContext),
                valueOrNotSelected(portfolioContext),
                valueOrNotSelected(resumeContext)
        );
        String content = chatClientBuilder.build()
                .prompt(prompt)
                .call()
                .content();

        return Arrays.stream(content.split("\\R"))
                .map(line -> line.replaceFirst("^\\s*\\d+[.)]\\s*", "").trim())
                .filter(line -> !line.isBlank())
                .limit(10)
                .toList();
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
}
