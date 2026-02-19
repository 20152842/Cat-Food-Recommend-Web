package com.catfood.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CalorieCalculationService 테스트")
class CalorieCalculationServiceTest {

    private CalorieCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CalorieCalculationService();
    }

    @Test
    @DisplayName("RER 계산: 4kg 고양이 기초대사량")
    void calculateRER_4kg() {
        double rer = service.calculateRER(4.0);
        // 70 × 4^0.75 = 70 × 2.828... ≈ 198
        assertThat(rer).isCloseTo(198.0, within(5.0));
    }

    @Test
    @DisplayName("생애 단계 계수: 신생 키튼(0~3개월) → 3.0")
    void lifeFactor_newbornKitten() {
        assertThat(service.getLifeFactor(2, "MALE", false)).isEqualTo(3.0);
    }

    @Test
    @DisplayName("생애 단계 계수: 성장기 키튼(4~11개월) → 2.5")
    void lifeFactor_growingKitten() {
        assertThat(service.getLifeFactor(6, "FEMALE", false)).isEqualTo(2.5);
    }

    @Test
    @DisplayName("생애 단계 계수: 성체 중성화 수컷 → 1.6")
    void lifeFactor_neuteredMaleAdult() {
        assertThat(service.getLifeFactor(24, "MALE", true)).isEqualTo(1.6);
    }

    @Test
    @DisplayName("생애 단계 계수: 성체 중성화 암컷 → 1.4")
    void lifeFactor_neuteredFemaleAdult() {
        assertThat(service.getLifeFactor(24, "FEMALE", true)).isEqualTo(1.4);
    }

    @Test
    @DisplayName("생애 단계 계수: 성체 미중성화 수컷 → 1.8")
    void lifeFactor_intactMaleAdult() {
        assertThat(service.getLifeFactor(24, "MALE", false)).isEqualTo(1.8);
    }

    @Test
    @DisplayName("생애 단계 계수: 노령묘(84개월+) → 1.4")
    void lifeFactor_senior() {
        assertThat(service.getLifeFactor(100, "MALE", true)).isEqualTo(1.4);
        assertThat(service.getLifeFactor(100, "FEMALE", false)).isEqualTo(1.4);
    }

    @Test
    @DisplayName("일일 권장 칼로리: 4kg 중성화 수컷 성체")
    void calculateDailyCalories_4kgNeuteredMale() {
        double daily = service.calculateDailyCalories(4.0, 24, "MALE", true);
        double expected = service.calculateRER(4.0) * 1.6;
        assertThat(daily).isCloseTo(expected, within(0.1));
    }

    @Test
    @DisplayName("생애 단계 키: 키튼(0~11개월)")
    void lifeStageKey_kitten() {
        assertThat(service.getLifeStageKey(1)).isEqualTo("KITTEN");
        assertThat(service.getLifeStageKey(11)).isEqualTo("KITTEN");
    }

    @Test
    @DisplayName("생애 단계 키: 성체(12~83개월)")
    void lifeStageKey_adult() {
        assertThat(service.getLifeStageKey(12)).isEqualTo("ADULT");
        assertThat(service.getLifeStageKey(83)).isEqualTo("ADULT");
    }

    @Test
    @DisplayName("생애 단계 키: 노령묘(84개월+)")
    void lifeStageKey_senior() {
        assertThat(service.getLifeStageKey(84)).isEqualTo("SENIOR");
        assertThat(service.getLifeStageKey(150)).isEqualTo("SENIOR");
    }
}
