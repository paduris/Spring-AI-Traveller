package com.spring.ai.springaitraveller.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private String query;
    private String aiResponse;
    private List<TravelData> relevantDestinations;
}
