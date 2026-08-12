package com.ssafy.b109.aivo.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.b109.aivo.interview.entity.Company;
import com.ssafy.b109.aivo.interview.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyResearchService {

    private static final int MAX_CONTEXT_LENGTH = 4_000;

    private final RestClient.Builder restClientBuilder;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CompanyRepository companyRepository;

    @Value("${naver.search.client-id:}")
    private String clientId;

    @Value("${naver.search.client-secret:}")
    private String clientSecret;

    public String getOrResearch(Company company) {
        // 1. company 유효성 검사해주기
        if (company == null) {
            return null;
        }

        // 2. 일주일 이내에 조사했으면 패스. 서순 중요함.
        if (company.getResearchContext() != null &&
                !company.getResearchContext().isBlank() &&
                company.getResearchUpdatedAt() != null &&
                company.getResearchUpdatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            return company.getResearchContext();
        }

        // 3. 조사 이전, 키 검사 ( 유무만 검사 )
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.debug("네이버 검색 키가 없어 기업 조사를 건너뜁니다: companyId={}", company.getId());
            return company.getResearchContext();
        }

        try {
            StringBuilder materials = new StringBuilder();
            StringBuilder sources = new StringBuilder();

            appendSearchResults(materials, sources, "news", company.getName(), "최신 기사");
            appendSearchResults(materials, sources, "webkr", company.getName() + " 채용", "채용공고/JD");
            appendSearchResults(materials, sources, "webkr", company.getName() + " 직무 채용", "직무/JD");

            if (materials.isEmpty()) {
                return company.getResearchContext();
            }

            String context = chatClientBuilder.build()
                    .prompt("""
                            너는 기업 리서치 요약자다.
                            아래 네이버 검색 결과만 근거로 면접 질문 생성에 사용할 기업 context를 작성해라.
                            공식 기업 홈페이지·공식 채용 페이지·채용공고·신뢰도 높은 언론 자료를 우선하라.
                            검색 결과에 없는 사실, 추측, 일반적인 기업 설명은 추가하지 마라.
                            최근 사업 방향, 주요 제품·서비스, 채용 직무, 요구 역량 중심으로 5개 이내 항목으로 요약하라.
                            결과는 일반 텍스트로만 작성하고 검색 과정이나 내부 판단은 작성하지 마라.
                            최대 1,500자 이내로 작성하라.

                            검색 결과:
                            %s
                            """.formatted(materials))
                    .call()
                    .content();

            if (context == null || context.isBlank()) {
                return company.getResearchContext();
            }

            context = context.trim();
            if (context.length() > MAX_CONTEXT_LENGTH) {
                context = context.substring(0, MAX_CONTEXT_LENGTH);
            }

            company.setResearchContext(context);
            company.setResearchSources(sources.toString().trim());
            company.setResearchUpdatedAt(LocalDateTime.now());
            companyRepository.save(company);
            return context;
        } catch (Exception exception) {
            log.warn("기업 정보 조사 실패. 기존 기업 context로 진행합니다: companyId={}", company.getId(), exception);
            return company.getResearchContext();
        }
    }

    /**
     * Naver PAI 호출단.
     * @param materials
     * @param sources
     * @param endpoint
     * @param query
     * @param category
     */
    private void appendSearchResults(
            StringBuilder materials,
            StringBuilder sources,
            String endpoint,
            String query,
            String category
    ) {
        String response = restClientBuilder.build()
                .get()
                .uri(UriComponentsBuilder
                        .fromUriString("https://naverapihub.apigw.ntruss.com/search/v1/" + endpoint)
                        .queryParam("query", query)
                        .queryParam("display", 5)
                        .queryParam("format", "json")
                        .queryParamIfPresent(
                                "sort",
                                "news".equals(endpoint) ? java.util.Optional.of("date") : java.util.Optional.empty()
                        )
                        .build()
                        .encode()
                        .toUri())
                .header("X-NCP-APIGW-API-KEY-ID", clientId)
                .header("X-NCP-APIGW-API-KEY", clientSecret)
                .retrieve()
                .body(String.class);

        try {
            JsonNode items = objectMapper.readTree(response).path("items");
            if (!items.isArray()) {
                return;
            }

            for (JsonNode item : items) {
                String title = clean(item.path("title").asText());
                String description = clean(item.path("description").asText());
                String link = item.path("originallink").asText(item.path("link").asText());

                if (title.isBlank() && description.isBlank()) {
                    continue;
                }

                materials.append("- [")
                        .append(category)
                        .append("] 제목: ")
                        .append(title)
                        .append(" / 내용: ")
                        .append(description)
                        .append(" / 출처: ")
                        .append(link)
                        .append("\n");
                if (!link.isBlank()) {
                    sources.append(link).append("\n");
                }
            }
        } catch (Exception exception) {
            log.debug("네이버 검색 결과 파싱 실패: endpoint={}, query={}", endpoint, query, exception);
        }
    }

    /**
     * 정규화
     * @param value
     * @return
     */
    private String clean(String value) {
        return value == null ? "" : value
                .replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
