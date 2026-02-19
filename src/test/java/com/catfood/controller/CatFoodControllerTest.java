package com.catfood.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CatFoodController 통합 테스트")
class CatFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정상 요청: 4kg 성체 중성화 수컷, 예산 50000원")
    void recommend_validRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "weightKg", 4.0,
                "ageMonths", 36,
                "gender", "MALE",
                "neutered", true,
                "monthlyBudget", 50000
        );

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyCalories").isNumber())
                .andExpect(jsonPath("$.rerCalories").isNumber())
                .andExpect(jsonPath("$.lifeFactor").value(1.6))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    @DisplayName("정상 요청: 1.5kg 키튼 암컷, 예산 30000원")
    void recommend_kittenRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "weightKg", 1.5,
                "ageMonths", 5,
                "gender", "FEMALE",
                "neutered", false,
                "monthlyBudget", 30000
        );

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifeFactor").value(2.5));
    }

    @Test
    @DisplayName("검증 오류: 체중 누락")
    void recommend_missingWeight() throws Exception {
        Map<String, Object> request = Map.of(
                "ageMonths", 24,
                "gender", "MALE",
                "neutered", true,
                "monthlyBudget", 50000
        );

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.weightKg").exists());
    }

    @Test
    @DisplayName("검증 오류: 잘못된 성별 값")
    void recommend_invalidGender() throws Exception {
        Map<String, Object> request = Map.of(
                "weightKg", 4.0,
                "ageMonths", 24,
                "gender", "UNKNOWN",
                "neutered", true,
                "monthlyBudget", 50000
        );

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예산 초과: 예산 1000원 → 추천 결과 없음")
    void recommend_budgetTooLow() throws Exception {
        Map<String, Object> request = Map.of(
                "weightKg", 4.0,
                "ageMonths", 24,
                "gender", "MALE",
                "neutered", true,
                "monthlyBudget", 1000
        );

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray());
    }
}
