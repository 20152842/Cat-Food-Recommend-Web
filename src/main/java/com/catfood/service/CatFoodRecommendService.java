package com.catfood.service;

import com.catfood.client.NaverShopItem;
import com.catfood.dto.FoodRecommendation;
import com.catfood.dto.RecommendRequest;
import com.catfood.dto.RecommendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 고양이 사료 추천 서비스 — 검색 전용.
 * 내부 DB 없이 네이버 쇼핑 검색 결과만 사용. 정확한 영양 정보는 비교 목록에서 사용자 입력으로 검증.
 */
@Service
public class CatFoodRecommendService {

    private static final Logger logger = LoggerFactory.getLogger(CatFoodRecommendService.class);

    private final CalorieCalculationService calorieService;
    private final NaverShoppingSearchService naverShoppingSearchService;

    public CatFoodRecommendService(CalorieCalculationService calorieService,
                                    NaverShoppingSearchService naverShoppingSearchService) {
        this.calorieService = calorieService;
        this.naverShoppingSearchService = naverShoppingSearchService;
    }

    /**
     * 검색어·고양이 정보로 네이버 쇼핑 검색 결과를 추천 형태로 반환.
     * 영양 정보(단백질/지방/하루급여량)는 검색 API에 없으므로 null → 비교 목록에서 사용자 입력 후 표시.
     */
    public RecommendResponse recommend(RecommendRequest request) {
        double weightKg = request.getWeightKg();
        int ageMonths = request.getAgeMonths();
        String gender = request.getGender();
        boolean neutered = request.getNeutered();

        double rer = calorieService.calculateRER(weightKg);
        double lifeFactor = calorieService.getLifeFactor(ageMonths, gender, neutered);
        double dailyCalories = rer * lifeFactor;
        String lifeStageKey = calorieService.getLifeStageKey(ageMonths);
        String lifeStageDesc = calorieService.getLifeStageDescription(ageMonths, gender, neutered);
        String formula = calorieService.generateFormula(weightKg, rer, lifeFactor, dailyCalories);

        logger.info("추천 요청 - 체중: {}kg, 나이: {}개월, 검색 전용", weightKg, ageMonths);

        if (!naverShoppingSearchService.isAvailable()) {
            return buildEmptyResponse(dailyCalories, rer, lifeFactor, lifeStageDesc, formula,
                    "네이버 쇼핑 API가 설정되지 않았습니다. application.properties에 naver.api.client-id, client-secret을 설정해주세요.");
        }

        String query = (request.getSearchQuery() != null && !request.getSearchQuery().isBlank())
                ? request.getSearchQuery().trim()
                : buildSearchQueryByLifeStage(lifeStageKey);
        List<FoodRecommendation> list = recommendFromSearch(query, dailyCalories);
        if (list.isEmpty()) {
            return buildEmptyResponse(dailyCalories, rer, lifeFactor, lifeStageDesc, formula,
                    "검색 결과가 없습니다. 검색어를 바꿔 보세요.");
        }
        return buildResponseWithSorts(dailyCalories, rer, lifeFactor, lifeStageDesc, formula, list);
    }

    private RecommendResponse buildEmptyResponse(double dailyCalories, double rer, double lifeFactor,
                                                  String lifeStageDesc, String formula, String message) {
        logger.warn(message);
        List<FoodRecommendation> empty = new ArrayList<>();
        return new RecommendResponse(
                dailyCalories, rer, lifeFactor, lifeStageDesc, formula,
                CalculationSourceDocument.FULL_DESCRIPTION,
                empty, empty, empty, message, empty);
    }

    /**
     * 네이버 쇼핑 검색 결과 → 추천 카드 목록.
     * 단백질/지방/하루급여량/하루비용은 null (비교 목록에서 입력 후 사용). 월 비용만 정렬용으로 추정치 사용.
     */
    private List<FoodRecommendation> recommendFromSearch(String query, double dailyCalories) {
        List<NaverShopItem> items = naverShoppingSearchService.search(query, 30);
        if (items == null || items.isEmpty()) return new ArrayList<>();

        List<FoodRecommendation> list = new ArrayList<>();
        for (NaverShopItem item : items) {
            String title = NaverShoppingSearchService.stripTitle(item.getTitle());
            if (title.length() < 2) continue;
            int lprice = parsePrice(item.getLprice());
            if (lprice <= 0) continue;

            boolean isWet = NaverShoppingSearchService.isWetFromTitle(
                    item.getTitle(), item.getCategory1(), item.getCategory2());
            double weightKg = NaverShoppingSearchService.parseWeightKg(item.getTitle(), isWet);
            double kcalPer100g = NaverShoppingSearchService.getDefaultKcalPer100g(isWet);
            int pricePerKg = weightKg > 0 ? (int) Math.round(lprice / weightKg) : lprice * 50;
            double dailyAmountGrams = dailyCalories / kcalPer100g * 100.0;
            int dailyCost = (int) Math.ceil(dailyAmountGrams / 1000.0 * pricePerKg);
            int monthlyCost = dailyCost * 30;
            double score = 0; // 정렬용으로만 사용, 검색 결과에서는 영양 미표시

            FoodRecommendation rec = new FoodRecommendation();
            rec.setFoodName(title);
            rec.setBrand(item.getBrand() != null && !item.getBrand().isEmpty() ? item.getBrand() : item.getMaker());
            rec.setType(isWet ? "WET" : "DRY");
            rec.setProductLink(item.getLink());
            rec.setImageUrl(item.getImage());
            rec.setProductPrice(lprice);
            rec.setFromRealSearch(true);
            rec.setReviewCount(0);
            rec.setScore(score);
            rec.setReason("");
            rec.setMonthlyCost(monthlyCost);
            rec.setProteinPercent(null);
            rec.setFatPercent(null);
            rec.setDailyAmountGrams(null);
            rec.setDailyCost(null);
            list.add(rec);
        }
        return list.stream().limit(5).collect(Collectors.toList());
    }

    private String buildSearchQueryByLifeStage(String lifeStageKey) {
        return switch (lifeStageKey) {
            case "KITTEN" -> "고양이 키튼 사료";
            case "SENIOR" -> "고양이 시니어 노령 사료";
            default -> "고양이 사료";
        };
    }

    private int parsePrice(String lprice) {
        if (lprice == null || lprice.isBlank()) return 0;
        try {
            return Integer.parseInt(lprice.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private RecommendResponse buildResponseWithSorts(double dailyCalories, double rer, double lifeFactor,
                                                     String lifeStageDesc, String formula,
                                                     List<FoodRecommendation> list) {
        List<FoodRecommendation> limited = list.stream().limit(5).collect(Collectors.toList());
        List<FoodRecommendation> byRank = new ArrayList<>(limited);
        List<FoodRecommendation> byPrice = limited.stream()
                .sorted(Comparator.comparingInt(FoodRecommendation::getMonthlyCost))
                .collect(Collectors.toList());
        List<FoodRecommendation> byReview = limited.stream()
                .sorted(Comparator.comparingInt(FoodRecommendation::getReviewCount).reversed()
                        .thenComparing(Comparator.comparingDouble(FoodRecommendation::getScore).reversed()))
                .collect(Collectors.toList());

        String reviewNote = "리뷰 수 정보는 제공되지 않아 추천 점수순으로 표시합니다.";
        return new RecommendResponse(
                dailyCalories, rer, lifeFactor, lifeStageDesc, formula,
                CalculationSourceDocument.FULL_DESCRIPTION,
                byRank, byPrice, byReview, reviewNote, byRank);
    }
}
