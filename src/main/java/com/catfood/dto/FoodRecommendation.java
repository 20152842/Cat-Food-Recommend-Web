package com.catfood.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 사료 추천 결과 DTO
 */
@Data
@NoArgsConstructor
public class FoodRecommendation {

    private int rank;
    private String foodName;
    private String brand;
    private String type;
    /** 검색 결과에서는 null. 비교 목록에서 사용자 입력 후 계산해 표시 */
    private Double dailyAmountGrams;
    private Integer dailyCost;
    private Integer monthlyCost;
    /** 검색 결과에서는 null. 비교 목록에서 사용자 입력값 표시 */
    private Double proteinPercent;
    private Double fatPercent;
    private String reason;
    private double score;

    /** 검색 결과 상품 최저가 (원). 비교 목록에서는 사용자 입력 가격 우선 */
    private Integer productPrice;
    /** 상품 상세 URL (네이버 쇼핑 등) */
    private String productLink;

    /** 실제 검색 기반 추천 시 상품 이미지 URL */
    private String imageUrl;

    /** 실제 검색 결과 여부 (프론트에서 구매 링크 등 표시용) */
    private Boolean fromRealSearch = false;

    /** 리뷰 수 (가격순/리뷰순 정렬용. 네이버 API 미제공 시 0) */
    private Integer reviewCount = 0;
}
