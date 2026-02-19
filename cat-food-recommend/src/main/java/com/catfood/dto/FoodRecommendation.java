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
    private double dailyAmountGrams;
    private int dailyCost;
    private int monthlyCost;
    private double proteinPercent;
    private double fatPercent;
    private String reason;
    private double score;
}
