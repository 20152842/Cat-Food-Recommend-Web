package com.catfood.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비교 목록 개별 항목 DTO.
 * 검색 결과에서 추가 시 링크/이름/이미지/가격 등이 채워지고,
 * 사용자가 입력한 정확한 영양 정보(단백질, 지방, 칼로리, 가격, 용량)를 담는다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonItemDto {

    private String id;
    private String productLink;
    private String productName;
    private String brand;
    private String imageUrl;
    /** 검색 시 최저가 (원). 사용자가 입력한 가격이 있으면 그걸 우선 */
    private Integer lprice;

    // ---- 사용자 입력(검증용) - 정확한 정보 우선 ----
    /** 단백질 (%) */
    private Double proteinPercent;
    /** 지방 (%) */
    private Double fatPercent;
    /** 100g당 칼로리 (kcal) */
    private Double kcalPer100g;
    /** 실제 구매가 (원) */
    private Integer price;
    /** 용량 (kg) */
    private Double weightKg;

    // ---- 서버 계산 (dailyCalories 기준) - 입력이 다 있을 때만 ----
    private Double dailyAmountGrams;
    private Integer dailyCost;
    private Integer monthlyCost;
}
