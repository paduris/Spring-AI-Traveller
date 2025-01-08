package com.spring.ai.springaitraveller.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@Builder
public class TravelData {
    private String id;
    private String destination;
    private String description;
    private String type;
    private String bestTimeToVisit;
    private Integer avgCostPerDay;
    private List<String> popularAttractions;

    public String getContentForEmbedding() {
        return String.format("%s - %s. Type: %s. Best time to visit: %s. Popular attractions: %s",
                destination,
                description,
                type,
                bestTimeToVisit,
                String.join(", ", popularAttractions));
    }
}
