package com.catfood.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사료 추천 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResponse {

    /**
     * 일일 권장 칼로리 (kcal)
     */
    private double dailyCalories;

    /**
     * 기초대사량 RER (kcal)
     */
    private double rerCalories;

    /**
     * 생애 단계 계수
     */
    private double lifeFactor;

    /**
     * 생애 단계 설명
     */
    private String lifeStageDescription;

    /**
     * 칼로리 계산식 설명
     */
    private String formulaDescription;

    /**
     * 추천 사료 목록 (최대 5개)
     */
    private List<FoodRecommendation> recommendations;
}
