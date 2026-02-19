package com.catfood.service;

import com.catfood.client.NaverShopItem;
import com.catfood.client.NaverShopSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 네이버 쇼핑 검색 API를 호출해 실제 상품 검색 결과를 가져옵니다.
 * 실제 검색 기반 추천 시 가격·링크·이미지를 이 결과로 채웁니다.
 */
@Service
public class NaverShoppingSearchService {

    private static final Logger logger = LoggerFactory.getLogger(NaverShoppingSearchService.class);
    private static final String SEARCH_SHOP_URL = "https://openapi.naver.com/v1/search/shop.json";
    private static final int DEFAULT_DISPLAY = 30;
    private static final double DEFAULT_KCAL_DRY_PER_100G = 385.0;
    private static final double DEFAULT_KCAL_WET_PER_100G = 85.0;
    private static final double DEFAULT_PROTEIN = 33.0;
    private static final double DEFAULT_FAT = 14.0;

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    /** 상품명에서 용량(kg/g) 추출용 */
    private static final Pattern WEIGHT_KG = Pattern.compile("(\\d+\\.?\\d*)\\s*(kg|키로|KG)");
    private static final Pattern WEIGHT_G = Pattern.compile("(\\d+)\\s*(g|그램|G)\\b");

    public NaverShoppingSearchService(
            RestTemplate restTemplate,
            @Value("${naver.api.client-id:}") String clientId,
            @Value("${naver.api.client-secret:}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId != null ? clientId.trim() : "";
        this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
    }

    /**
     * API 키가 설정되어 있으면 실제 검색 사용 가능.
     */
    public boolean isAvailable() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    /**
     * 네이버 쇼핑에서 검색어로 상품 목록을 조회합니다.
     *
     * @param query 검색어 (예: "고양이 사료", "고양이 키튼 건식")
     * @param display 최대 개수 (1~100)
     * @return 상품 목록 (실패 시 빈 리스트)
     */
    public List<NaverShopItem> search(String query, int display) {
        if (!isAvailable()) {
            logger.warn("네이버 API 미설정으로 검색 스킵");
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_SHOP_URL)
                .queryParam("query", query)
                .queryParam("display", Math.min(100, Math.max(1, display)))
                .queryParam("sort", "sim")
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        try {
            ResponseEntity<NaverShopSearchResponse> res = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    NaverShopSearchResponse.class);
            NaverShopSearchResponse body = res.getBody();
            if (body != null && body.getItems() != null) {
                logger.info("네이버 쇼핑 검색 성공 - query: {}, 결과: {}건", query, body.getItems().size());
                return body.getItems();
            }
        } catch (Exception e) {
            logger.warn("네이버 쇼핑 검색 실패 - query: {}, error: {}", query, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 검색 (display 기본값 사용)
     */
    public List<NaverShopItem> search(String query) {
        return search(query, DEFAULT_DISPLAY);
    }

    /**
     * 상품명에서 제목 태그 제거 (네이버는 검색어를 &lt;b&gt;로 감쌈)
     */
    public static String stripTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * 상품명에서 용량(kg)을 추출합니다. 없으면 건식 2kg, 습식 0.085kg 추정.
     */
    public static double parseWeightKg(String title, boolean isWet) {
        if (title == null) title = "";
        Matcher kg = WEIGHT_KG.matcher(title);
        if (kg.find()) {
            try {
                return Double.parseDouble(kg.group(1));
            } catch (NumberFormatException ignored) {}
        }
        Matcher g = WEIGHT_G.matcher(title);
        if (g.find()) {
            try {
                return Integer.parseInt(g.group(1)) / 1000.0;
            } catch (NumberFormatException ignored) {}
        }
        return isWet ? 0.085 : 2.0;
    }

    /**
     * 상품명·카테고리로 건식/습식 여부 추정.
     */
    public static boolean isWetFromTitle(String title, String category1, String category2) {
        if (title == null) title = "";
        String t = title + " " + (category1 != null ? category1 : "") + " " + (category2 != null ? category2 : "");
        return t.matches("(?i).*캔|파우치|습식|웻|wet|소프트|참치.*");
    }

    public static double getDefaultKcalPer100g(boolean isWet) {
        return isWet ? DEFAULT_KCAL_WET_PER_100G : DEFAULT_KCAL_DRY_PER_100G;
    }

    public static double getDefaultProtein() {
        return DEFAULT_PROTEIN;
    }

    public static double getDefaultFat() {
        return DEFAULT_FAT;
    }
}
