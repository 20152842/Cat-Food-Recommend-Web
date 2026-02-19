package com.catfood.service;

import com.catfood.dto.CompareAddRequest;
import com.catfood.dto.CompareUpdateRequest;
import com.catfood.dto.ComparisonItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비교 목록 인메모리 저장.
 * basketId(클라이언트 생성)별로 최대 5개까지 보관.
 * 정확한 정보는 사용자 입력으로 검증.
 */
@Service
public class ComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonService.class);
    private static final int MAX_ITEMS = 5;

    /** basketId -> list of items */
    private final Map<String, List<ComparisonItemDto>> store = new ConcurrentHashMap<>();

    public List<ComparisonItemDto> getList(String basketId, Double dailyCalories) {
        List<ComparisonItemDto> list = store.getOrDefault(basketId, new ArrayList<>());
        if (dailyCalories != null && dailyCalories > 0) {
            list = list.stream().map(item -> computeDerived(item, dailyCalories)).toList();
        }
        return new ArrayList<>(list);
    }

    public ComparisonItemDto add(String basketId, CompareAddRequest req) {
        List<ComparisonItemDto> list = store.computeIfAbsent(basketId, k -> new ArrayList<>());
        if (list.size() >= MAX_ITEMS) {
            logger.warn("비교 목록 최대 {}개 초과", MAX_ITEMS);
            return null;
        }
        String id = UUID.randomUUID().toString();
        ComparisonItemDto dto = new ComparisonItemDto(
                id,
                req.getProductLink(),
                req.getProductName(),
                req.getBrand(),
                req.getImageUrl(),
                req.getLprice(),
                req.getProteinPercent(),
                req.getFatPercent(),
                req.getKcalPer100g(),
                req.getPrice(),
                req.getWeightKg(),
                null, null, null
        );
        list.add(dto);
        logger.info("비교 목록 추가 - basketId: {}, productName: {}, 현재 개수: {}", basketId, req.getProductName(), list.size());
        return dto;
    }

    public boolean update(String basketId, String itemId, CompareUpdateRequest req) {
        List<ComparisonItemDto> list = store.get(basketId);
        if (list == null) return false;
        for (ComparisonItemDto item : list) {
            if (item.getId().equals(itemId)) {
                if (req.getProteinPercent() != null) item.setProteinPercent(req.getProteinPercent());
                if (req.getFatPercent() != null) item.setFatPercent(req.getFatPercent());
                if (req.getKcalPer100g() != null) item.setKcalPer100g(req.getKcalPer100g());
                if (req.getPrice() != null) item.setPrice(req.getPrice());
                if (req.getWeightKg() != null) item.setWeightKg(req.getWeightKg());
                return true;
            }
        }
        return false;
    }

    public boolean remove(String basketId, String itemId) {
        List<ComparisonItemDto> list = store.get(basketId);
        if (list == null) return false;
        boolean removed = list.removeIf(item -> item.getId().equals(itemId));
        if (removed && list.isEmpty()) store.remove(basketId);
        return removed;
    }

    private ComparisonItemDto computeDerived(ComparisonItemDto item, double dailyCalories) {
        Double kcal = item.getKcalPer100g();
        Integer price = item.getPrice();
        Double weightKg = item.getWeightKg();
        if (kcal == null || kcal <= 0 || weightKg == null || weightKg <= 0) return item;
        double dailyGrams = dailyCalories / kcal * 100.0;
        int pricePerKg = (int) Math.round((price != null ? price : item.getLprice() != null ? item.getLprice() : 0) / weightKg);
        int dailyCost = (int) Math.ceil(dailyGrams / 1000.0 * pricePerKg);
        int monthlyCost = dailyCost * 30;
        ComparisonItemDto out = new ComparisonItemDto(
                item.getId(), item.getProductLink(), item.getProductName(), item.getBrand(),
                item.getImageUrl(), item.getLprice(), item.getProteinPercent(), item.getFatPercent(),
                item.getKcalPer100g(), item.getPrice(), item.getWeightKg(),
                Math.round(dailyGrams * 10.0) / 10.0, dailyCost, monthlyCost
        );
        return out;
    }

    public int getMaxItems() {
        return MAX_ITEMS;
    }
}
