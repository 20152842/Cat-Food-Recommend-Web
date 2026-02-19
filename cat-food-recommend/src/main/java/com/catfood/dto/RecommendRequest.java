package com.catfood.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 사료 추천 요청 DTO
 */
@Data
public class RecommendRequest {

    /**
     * 체중 (kg)
     */
    @NotNull(message = "체중은 필수 입력 항목입니다.")
    @DecimalMin(value = "0.1", message = "체중은 0.1kg 이상이어야 합니다.")
    @DecimalMax(value = "20.0", message = "체중은 20kg 이하로 입력해주세요.")
    private Double weightKg;

    /**
     * 나이 (개월 수)
     */
    @NotNull(message = "나이는 필수 입력 항목입니다.")
    @Min(value = 1, message = "나이는 1개월 이상이어야 합니다.")
    @Max(value = 300, message = "나이는 300개월(25년) 이하로 입력해주세요.")
    private Integer ageMonths;

    /**
     * 성별: MALE(수컷), FEMALE(암컷)
     */
    @NotNull(message = "성별은 필수 입력 항목입니다.")
    @Pattern(regexp = "MALE|FEMALE", message = "성별은 MALE 또는 FEMALE이어야 합니다.")
    private String gender;

    /**
     * 중성화 여부
     */
    @NotNull(message = "중성화 여부는 필수 입력 항목입니다.")
    private Boolean neutered;

    /**
     * 월 예산 (원)
     */
    @NotNull(message = "월 예산은 필수 입력 항목입니다.")
    @Min(value = 1000, message = "월 예산은 1,000원 이상이어야 합니다.")
    @Max(value = 1000000, message = "월 예산은 1,000,000원 이하로 입력해주세요.")
    private Integer monthlyBudget;
}
