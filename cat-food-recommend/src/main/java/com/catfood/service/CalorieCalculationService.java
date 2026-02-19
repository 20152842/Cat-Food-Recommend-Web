package com.catfood.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 고양이 일일 권장 칼로리 계산 서비스
 *
 * 미국 국립연구위원회(NRC) 및 미국반려동물협회(AAFCO) 기반 공식 사용:
 *   RER (기초대사량) = 70 × 체중(kg)^0.75  kcal/일
 *   MER (유지에너지요구량) = RER × 생애 단계 계수
 *
 * 생애 단계 계수:
 *   - 신생 키튼 (0~3개월): 3.0
 *   - 성장기 키튼 (4~11개월): 2.5
 *   - 성체 중성화 수컷 (12~83개월): 1.6
 *   - 성체 중성화 암컷 (12~83개월): 1.4
 *   - 성체 미중성화 수컷 (12~83개월): 1.8
 *   - 성체 미중성화 암컷 (12~83개월): 1.6
 *   - 노령묘 (84개월 이상, 7세+): 1.4
 */
@Service
public class CalorieCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(CalorieCalculationService.class);

    /**
     * 기초대사량(RER)을 계산합니다.
     *
     * @param weightKg 체중 (kg)
     * @return RER (kcal/일)
     */
    public double calculateRER(double weightKg) {
        return 70.0 * Math.pow(weightKg, 0.75);
    }

    /**
     * 일일 권장 칼로리(MER)를 계산합니다.
     *
     * @param weightKg  체중 (kg)
     * @param ageMonths 나이 (개월 수)
     * @param gender    성별 ("MALE" or "FEMALE")
     * @param neutered  중성화 여부
     * @return MER (kcal/일)
     */
    public double calculateDailyCalories(double weightKg, int ageMonths, String gender, boolean neutered) {
        double rer = calculateRER(weightKg);
        double factor = getLifeFactor(ageMonths, gender, neutered);
        double mer = rer * factor;

        logger.info("칼로리 계산 - 체중: {}kg, 나이: {}개월, 성별: {}, 중성화: {} → RER: {}, 계수: {}, MER: {}",
                weightKg, ageMonths, gender, neutered,
                String.format("%.1f", rer), factor, String.format("%.1f", mer));

        return mer;
    }

    /**
     * 생애 단계 계수를 반환합니다.
     *
     * @param ageMonths 나이 (개월 수)
     * @param gender    성별
     * @param neutered  중성화 여부
     * @return 생애 단계 계수
     */
    public double getLifeFactor(int ageMonths, String gender, boolean neutered) {
        if (ageMonths < 4) {
            return 3.0;
        }
        if (ageMonths < 12) {
            return 2.5;
        }
        if (ageMonths >= 84) {
            return 1.4;
        }
        boolean isMale = "MALE".equalsIgnoreCase(gender);
        if (neutered) {
            return isMale ? 1.6 : 1.4;
        } else {
            return isMale ? 1.8 : 1.6;
        }
    }

    /**
     * 생애 단계 설명 문자열을 반환합니다.
     */
    public String getLifeStageDescription(int ageMonths, String gender, boolean neutered) {
        if (ageMonths < 4) return "신생 키튼 (0~3개월)";
        if (ageMonths < 12) return "성장기 키튼 (4~11개월)";
        if (ageMonths >= 84) return "노령묘 (7세 이상)";
        String genderStr = "MALE".equalsIgnoreCase(gender) ? "수컷" : "암컷";
        String neuteredStr = neutered ? "중성화" : "미중성화";
        return String.format("성체 %s %s (1~7세)", neuteredStr, genderStr);
    }

    /**
     * 생애 단계 키를 반환합니다. (사료 필터링에 사용)
     */
    public String getLifeStageKey(int ageMonths) {
        if (ageMonths < 12) return "KITTEN";
        if (ageMonths >= 84) return "SENIOR";
        return "ADULT";
    }

    /**
     * 칼로리 계산식 설명을 생성합니다.
     */
    public String generateFormula(double weightKg, double rer, double lifeFactor, double mer) {
        return String.format(
                "NRC/AAFCO 권장 칼로리 계산 공식\n" +
                "① 기초대사량(RER) = 70 × 체중(kg)^0.75\n" +
                "   = 70 × %.1f^0.75 = %.1f kcal/일\n" +
                "② 일일 권장량(MER) = RER × 생애 단계 계수\n" +
                "   = %.1f × %.1f = %.1f kcal/일",
                weightKg, rer, rer, lifeFactor, mer
        );
    }
}
