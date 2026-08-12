package com.ssafy.b109.aivo.llm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioSummaryGenerator {

    private static final int MAX_SOURCE_LENGTH = 12_000;

    private final ChatClient.Builder chatClientBuilder;

    public String summarize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String source = text.length() > MAX_SOURCE_LENGTH ? text.substring(0, MAX_SOURCE_LENGTH) : text;
        String prompt = """
                다음 포트폴리오 내용을 면접 질문 생성을 위한 context로 요약해.
                프로젝트명, 기술스택, 담당 역할, 문제 해결, 성과, 협업 경험 위주로 1000자 이내로 정리해.
                사용자에게 그대로 노출되는 텍스트이므로 마크다운 문법을 절대 사용하지 마라.
                굵게 표시(**), 제목(#), 목록 기호(-, *, 숫자 목록), 코드블록, 표 없이 일반 문장만 반환하라.
                줄바꿈이 필요하면 문단 구분만 사용하고, 장식용 특수문자는 사용하지 마라.

                %s
                """.formatted(source);

        return chatClientBuilder.build()
                .prompt(prompt)
                .call()
                .content()
                .trim();
    }
}
