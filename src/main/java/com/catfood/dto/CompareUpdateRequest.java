package com.catfood.dto;

import lombok.Data;

/**
 * 비교 목록 항목 수정 요청. 사용자가 입력한 정확한 정보만 전송.
 */
@Data
public class CompareUpdateRequest {

    private Double proteinPercent;
    private Double fatPercent;
    private Double kcalPer100g;
    private Integer price;
    private Double weightKg;
}
