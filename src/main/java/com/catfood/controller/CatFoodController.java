package com.catfood.controller;

import com.catfood.dto.RecommendRequest;
import com.catfood.dto.RecommendResponse;
import com.catfood.service.CatFoodRecommendService;
import com.catfood.service.NaverShoppingSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 고양이 사료 추천 API 컨트롤러
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatFoodController {

    private static final Logger logger = LoggerFactory.getLogger(CatFoodController.class);

    private final CatFoodRecommendService recommendService;
    private final NaverShoppingSearchService naverShoppingSearchService;

    public CatFoodController(CatFoodRecommendService recommendService,
                             NaverShoppingSearchService naverShoppingSearchService) {
        this.recommendService = recommendService;
        this.naverShoppingSearchService = naverShoppingSearchService;
    }

    /**
     * 실제 검색 기반 추천 사용 가능 여부 (네이버 API 설정 시 true)
     */
    @GetMapping("/real-search-available")
    public ResponseEntity<Map<String, Boolean>> realSearchAvailable() {
        return ResponseEntity.ok(Map.of("available", naverShoppingSearchService.isAvailable()));
    }

    /**
     * 사료 추천 API
     *
     * @param request 고양이 정보 (체중, 나이, 성별, 중성화여부, 예산)
     * @return 추천 사료 목록 및 칼로리 계산 결과
     */
    @PostMapping("/recommend")
    public ResponseEntity<RecommendResponse> recommend(@Valid @RequestBody RecommendRequest request) {
        logger.info("사료 추천 요청 - 체중: {}kg, 나이: {}개월, 성별: {}, 중성화: {}, 예산: {}원",
                request.getWeightKg(), request.getAgeMonths(),
                request.getGender(), request.getNeutered(), request.getMonthlyBudget());

        RecommendResponse response = recommendService.recommend(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 입력 검증 오류 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("입력 검증 오류 발생: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
