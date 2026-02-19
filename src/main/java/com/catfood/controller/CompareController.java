package com.catfood.controller;

import com.catfood.dto.CompareAddRequest;
import com.catfood.dto.CompareUpdateRequest;
import com.catfood.dto.ComparisonItemDto;
import com.catfood.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 비교 목록 API. basketId는 쿼리 또는 헤더로 전달 (클라이언트 생성 UUID).
 */
@RestController
@RequestMapping("/api/compare")
@CrossOrigin(origins = "*")
public class CompareController {

    private final ComparisonService comparisonService;

    public CompareController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping
    public ResponseEntity<List<ComparisonItemDto>> list(
            @RequestParam String basketId,
            @RequestParam(required = false) Double dailyCalories) {
        List<ComparisonItemDto> list = comparisonService.getList(basketId, dailyCalories);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestParam String basketId, @RequestBody CompareAddRequest req) {
        if (req.getProductLink() == null || req.getProductLink().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "productLink 필수"));
        }
        ComparisonItemDto added = comparisonService.add(basketId, req);
        if (added == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "비교 목록은 최대 " + comparisonService.getMaxItems() + "개까지 가능합니다."));
        }
        return ResponseEntity.ok(added);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> update(
            @RequestParam String basketId,
            @PathVariable String itemId,
            @RequestBody CompareUpdateRequest req) {
        boolean ok = comparisonService.update(basketId, itemId, req);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> remove(@RequestParam String basketId, @PathVariable String itemId) {
        boolean ok = comparisonService.remove(basketId, itemId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/max")
    public ResponseEntity<Map<String, Integer>> max() {
        return ResponseEntity.ok(Map.of("maxItems", comparisonService.getMaxItems()));
    }
}
