package com.catfood.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 고양이 사료 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatFood {

    private String id;
    private String name;
    private String brand;

    /**
     * 사료 종류: DRY(건식), WET(습식)
     */
    private String type;

    /**
     * 적합 생애 단계: KITTEN(키튼), ADULT(성체), SENIOR(노령), ALL(전 연령)
     */
    private String lifeStage;

    /**
     * 100g당 칼로리 (kcal)
     */
    private double kcalPer100g;

    /**
     * kg당 가격 (원)
     */
    private int pricePerKg;

    /**
     * 단백질 함량 (%)
     */
    private double proteinPercent;

    /**
     * 지방 함량 (%)
     */
    private double fatPercent;

    private String description;
}
