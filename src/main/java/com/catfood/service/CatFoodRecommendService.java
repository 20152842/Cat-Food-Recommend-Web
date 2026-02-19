package com.catfood.service;

import com.catfood.dto.FoodRecommendation;
import com.catfood.dto.RecommendRequest;
import com.catfood.dto.RecommendResponse;
import com.catfood.model.CatFood;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 고양이 사료 추천 서비스
 *
 * 추천 점수 산정 기준:
 *   - 단백질 함량 (60%): 고양이는 육식 동물이므로 단백질이 가장 중요
 *   - 예산 여유율 (40%): 예산 대비 비용이 낮을수록 점수 상승
 */
@Service
public class CatFoodRecommendService {

    private static final Logger logger = LoggerFactory.getLogger(CatFoodRecommendService.class);

    private final CalorieCalculationService calorieService;
    private final List<CatFood> catFoodDatabase;

    public CatFoodRecommendService(CalorieCalculationService calorieService) {
        this.calorieService = calorieService;
        this.catFoodDatabase = initializeFoodDatabase();
    }

    /**
     * 고양이 정보를 기반으로 사료를 추천합니다.
     */
    public RecommendResponse recommend(RecommendRequest request) {
        double weightKg = request.getWeightKg();
        int ageMonths = request.getAgeMonths();
        String gender = request.getGender();
        boolean neutered = request.getNeutered();
        int monthlyBudget = request.getMonthlyBudget();

        double rer = calorieService.calculateRER(weightKg);
        double lifeFactor = calorieService.getLifeFactor(ageMonths, gender, neutered);
        double dailyCalories = rer * lifeFactor;
        String lifeStageKey = calorieService.getLifeStageKey(ageMonths);
        String lifeStageDesc = calorieService.getLifeStageDescription(ageMonths, gender, neutered);
        String formula = calorieService.generateFormula(weightKg, rer, lifeFactor, dailyCalories);

        logger.info("추천 요청 - 체중: {}kg, 나이: {}개월, 성별: {}, 중성화: {}, 예산: {}원, 일일칼로리: {}kcal",
                weightKg, ageMonths, gender, neutered, monthlyBudget, String.format("%.1f", dailyCalories));

        List<FoodRecommendation> recommendations = new ArrayList<>();

        for (CatFood food : catFoodDatabase) {
            if (!isAgeAppropriate(food, lifeStageKey)) continue;

            double dailyAmountGrams = dailyCalories / food.getKcalPer100g() * 100.0;
            double dailyCostDouble = dailyAmountGrams / 1000.0 * food.getPricePerKg();
            int dailyCost = (int) Math.ceil(dailyCostDouble);
            int monthlyCost = dailyCost * 30;

            if (monthlyCost > monthlyBudget) continue;

            double score = calculateScore(food, monthlyCost, monthlyBudget);
            String reason = generateReason(food, monthlyCost, monthlyBudget, lifeStageKey);

            FoodRecommendation rec = new FoodRecommendation();
            rec.setFoodName(food.getName());
            rec.setBrand(food.getBrand());
            rec.setType(food.getType());
            rec.setDailyAmountGrams(Math.round(dailyAmountGrams * 10.0) / 10.0);
            rec.setDailyCost(dailyCost);
            rec.setMonthlyCost(monthlyCost);
            rec.setProteinPercent(food.getProteinPercent());
            rec.setFatPercent(food.getFatPercent());
            rec.setScore(score);
            rec.setReason(reason);

            recommendations.add(rec);
        }

        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        List<FoodRecommendation> top5 = recommendations.stream().limit(5).collect(Collectors.toList());

        for (int i = 0; i < top5.size(); i++) {
            top5.get(i).setRank(i + 1);
        }

        logger.info("추천 완료 - 후보 {}개 중 {}개 추천", recommendations.size(), top5.size());

        if (top5.isEmpty()) {
            logger.warn("예산 {}원 내에서 적합한 사료를 찾지 못함", monthlyBudget);
        }

        return new RecommendResponse(dailyCalories, rer, lifeFactor, lifeStageDesc, formula, top5);
    }

    /**
     * 나이 기준으로 사료 적합성을 확인합니다.
     *
     * - KITTEN 사료: 키튼(0~11개월)에게만 적합
     * - ADULT 사료: 성체(12개월+) 및 노령묘에게 적합
     * - SENIOR 사료: 노령묘(84개월+)에게만 적합
     * - ALL 사료: 전 연령 적합
     */
    private boolean isAgeAppropriate(CatFood food, String lifeStageKey) {
        return switch (food.getLifeStage()) {
            case "ALL" -> true;
            case "KITTEN" -> "KITTEN".equals(lifeStageKey);
            case "ADULT" -> "ADULT".equals(lifeStageKey) || "SENIOR".equals(lifeStageKey);
            case "SENIOR" -> "SENIOR".equals(lifeStageKey);
            default -> false;
        };
    }

    /**
     * 추천 점수를 계산합니다.
     * 단백질 함량 60% + 예산 여유율 40%
     */
    private double calculateScore(CatFood food, int monthlyCost, int monthlyBudget) {
        double proteinScore = food.getProteinPercent();
        double budgetRemainingRatio = (double)(monthlyBudget - monthlyCost) / monthlyBudget * 100.0;
        return proteinScore * 0.6 + budgetRemainingRatio * 0.4;
    }

    /**
     * 추천 이유 문자열을 생성합니다.
     */
    private String generateReason(CatFood food, int monthlyCost, int monthlyBudget, String lifeStageKey) {
        List<String> reasons = new ArrayList<>();

        if (food.getProteinPercent() >= 38.0) {
            reasons.add("고단백 프리미엄 사료");
        } else if (food.getProteinPercent() >= 33.0) {
            reasons.add("균형 잡힌 단백질 함량");
        }

        int budgetUsagePercent = monthlyCost * 100 / monthlyBudget;
        if (budgetUsagePercent <= 50) {
            reasons.add("예산 절약형 (예산의 " + budgetUsagePercent + "% 사용)");
        } else if (budgetUsagePercent <= 75) {
            reasons.add("합리적인 가격 (예산의 " + budgetUsagePercent + "% 사용)");
        }

        if ("KITTEN".equals(lifeStageKey) && "KITTEN".equals(food.getLifeStage())) {
            reasons.add("성장기 전용 영양 설계");
        }
        if ("SENIOR".equals(lifeStageKey) && "SENIOR".equals(food.getLifeStage())) {
            reasons.add("노령묘 맞춤 영양 설계");
        }
        if ("WET".equals(food.getType())) {
            reasons.add("수분 보충에 도움");
        }

        return reasons.isEmpty() ? "기본 영양 균형 충족" : String.join(", ", reasons);
    }

    /**
     * 사료 데이터베이스를 초기화합니다.
     *
     * 데이터 기준: 각 브랜드 공식 영양 성분표 참고 (가격은 국내 유통 평균가 기준)
     * kcalPer100g: 100g당 칼로리 (건식 ~350~450kcal, 습식 ~70~100kcal)
     */
    private List<CatFood> initializeFoodDatabase() {
        List<CatFood> db = new ArrayList<>();

        // === 건식 사료 (DRY) ===

        // 전 연령 / 키튼 건식
        db.add(new CatFood("rc-kitten", "로얄캐닌 키튼", "Royal Canin", "DRY", "KITTEN",
                390, 62000, 32.0, 17.0, "성장기 전용 DHA·EPA 강화 공식"));
        db.add(new CatFood("orijen-kitten", "오리젠 키튼", "Orijen", "DRY", "KITTEN",
                418, 92000, 42.0, 20.0, "그레인프리 고단백 키튼 전용"));
        db.add(new CatFood("nc-kitten", "내추럴코어 유기농 키튼", "Natural Core", "DRY", "KITTEN",
                385, 68000, 37.0, 15.0, "유기농 원료 성장기 사료"));

        // 성체 건식
        db.add(new CatFood("rc-indoor", "로얄캐닌 인도어 어덜트", "Royal Canin", "DRY", "ADULT",
                390, 60000, 30.0, 12.0, "실내 고양이 체중 관리 최적화"));
        db.add(new CatFood("hills-adult", "힐스 사이언스다이어트 어덜트", "Hill's", "DRY", "ADULT",
                360, 55000, 31.5, 12.1, "수의사 추천 균형 영양 공식"));
        db.add(new CatFood("orijen-adult", "오리젠 캣 & 키튼", "Orijen", "DRY", "ALL",
                418, 90000, 40.0, 20.0, "신선 육류 85% 그레인프리"));
        db.add(new CatFood("acana-prairie", "아카나 와일드프레리", "Acana", "DRY", "ADULT",
                394, 75000, 37.0, 18.0, "캐나다산 신선 가금류 고단백"));
        db.add(new CatFood("nc-adult", "내추럴코어 유기농 어덜트", "Natural Core", "DRY", "ADULT",
                385, 65000, 36.0, 15.0, "USDA 인증 유기농 원료 사용"));
        db.add(new CatFood("hiq-adult", "하이큐 슈프림 어덜트", "Hiq", "DRY", "ADULT",
                375, 45000, 34.0, 16.0, "합리적인 가격의 고단백 사료"));
        db.add(new CatFood("mb-adult", "모닝블루 어덜트", "Morningblue", "DRY", "ADULT",
                370, 35000, 33.0, 14.0, "국내 브랜드 가성비 건식 사료"));
        db.add(new CatFood("pp-adult", "퓨리나 프로플랜 어덜트", "Purina", "DRY", "ADULT",
                385, 50000, 35.0, 13.0, "장 건강 특화 프리바이오틱스 함유"));
        db.add(new CatFood("ziwi-adult", "지위픽 에어드라이 어덜트", "Ziwi Peak", "DRY", "ADULT",
                430, 98000, 43.0, 22.0, "뉴질랜드산 에어드라이 초고단백"));
        db.add(new CatFood("wc-adult", "웰치스 그레인프리 어덜트", "Welchis", "DRY", "ADULT",
                380, 40000, 35.0, 14.0, "국내 그레인프리 합리적 선택"));

        // 노령 건식
        db.add(new CatFood("rc-senior", "로얄캐닌 에이징 +12", "Royal Canin", "DRY", "SENIOR",
                350, 65000, 28.0, 10.0, "12세 이상 노령묘 신장 기능 고려"));
        db.add(new CatFood("hills-senior", "힐스 사이언스다이어트 시니어", "Hill's", "DRY", "SENIOR",
                340, 58000, 27.5, 9.5, "노령묘 관절·신장 건강 지원"));

        // === 습식 사료 (WET) ===

        // 전 연령 / 성체 습식
        db.add(new CatFood("inaba-adult", "이나바 CIAO 참치&닭", "Inaba", "WET", "ADULT",
                85, 25000, 14.0, 2.0, "일본산 고양이 기호성 최고"));
        db.add(new CatFood("yamaha-adult", "야마하시 참치&가다랑어", "Yamahashi", "WET", "ADULT",
                80, 20000, 12.0, 1.5, "신선한 해산물 기반 습식"));
        db.add(new CatFood("rc-wet", "로얄캐닌 웨트 어덜트", "Royal Canin", "WET", "ADULT",
                88, 50000, 13.0, 3.0, "수분·영양 균형 설계 파우치"));
        db.add(new CatFood("np-wet", "뉴트리플랜 그레인프리 캔", "Nutriplan", "WET", "ALL",
                92, 30000, 15.0, 2.5, "국내산 그레인프리 합리적 습식"));
        db.add(new CatFood("jeking-wet", "제왕 더 리얼 참치", "Jeking", "WET", "ADULT",
                95, 35000, 18.0, 2.0, "고단백 저지방 실속형 습식"));
        db.add(new CatFood("inaba-kitten", "이나바 CIAO 키튼", "Inaba", "WET", "KITTEN",
                90, 28000, 15.0, 2.5, "성장기 DHA 강화 키튼 파우치"));

        return db;
    }
}
