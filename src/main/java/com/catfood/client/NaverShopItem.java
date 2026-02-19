package com.catfood.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 네이버 쇼핑 검색 API 개별 상품 응답 DTO
 * https://developers.naver.com/docs/serviceapi/search/shopping/shopping.md
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopItem {

    private String title;
    private String link;
    private String image;

    /** 최저가 (문자열로 내려옴) */
    @JsonProperty("lprice")
    private String lprice;

    @JsonProperty("hprice")
    private String hprice;

    @JsonProperty("mallName")
    private String mallName;

    @JsonProperty("productId")
    private String productId;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("maker")
    private String maker;

    @JsonProperty("category1")
    private String category1;

    @JsonProperty("category2")
    private String category2;
}
