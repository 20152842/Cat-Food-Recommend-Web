package com.catfood.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 네이버 쇼핑 검색 API 전체 응답 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopSearchResponse {

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("start")
    private Integer start;

    @JsonProperty("display")
    private Integer display;

    @JsonProperty("items")
    private List<NaverShopItem> items;
}
