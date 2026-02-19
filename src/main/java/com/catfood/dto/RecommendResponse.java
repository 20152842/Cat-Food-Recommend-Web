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
     * 하루 급여량·하루 비용·월 비용 계산 출처 및 신빙성 안내
     */
    private String calculationSourceDescription;

    /**
     * 랭킹순(추천순) 목록 — 네이버 검색 시 검색 순서/정확도순, 내부 DB 시 추천 점수순
     */
    private List<FoodRecommendation> recommendationsByRank;

    /**
     * 가격순 목록 — 월 비용 낮은 순
     */
    private List<FoodRecommendation> recommendationsByPrice;

    /**
     * 리뷰순 목록 — 리뷰 수 많은 순 (API 미제공 시 추천 점수순으로 대체)
     */
    private List<FoodRecommendation> recommendationsByReview;

    /**
     * 리뷰순 선택 시 안내 문구 (리뷰 데이터 없을 때만 값 있음)
     */
    private String reviewSortNote;

    /**
     * @deprecated 기본 보기용. recommendationsByRank 와 동일
     */
    @Deprecated
    private List<FoodRecommendation> recommendations;
}
