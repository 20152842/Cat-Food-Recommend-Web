package com.catfood.dto;

import lombok.Data;

/**
 * 비교 목록에 추가 요청. 검색 결과에서 선택한 상품 정보 + (선택) 사용자 입력 정보.
 */
@Data
public class CompareAddRequest {

    private String productLink;
    private String productName;
    private String brand;
    private String imageUrl;
    private Integer lprice;

    private Double proteinPercent;
    private Double fatPercent;
    private Double kcalPer100g;
    private Integer price;
    private Double weightKg;
}
