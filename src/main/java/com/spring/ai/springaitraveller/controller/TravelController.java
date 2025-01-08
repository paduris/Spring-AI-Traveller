package com.spring.ai.springaitraveller.controller;

import com.spring.ai.springaitraveller.model.SearchResponse;
import com.spring.ai.springaitraveller.model.TravelData;
import com.spring.ai.springaitraveller.service.TravelDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel")
public class TravelController {

    private final TravelDataService travelDataService;

    public TravelController(TravelDataService travelDataService) {
        this.travelDataService = travelDataService;
    }

    @PostMapping("/ingest")
    public void ingestData(@RequestBody List<TravelData> travelDataList) {
        travelDataService.ingestData(travelDataList);
    }

    @GetMapping("/search")
    public SearchResponse searchTravel(@RequestParam String query) {
        return travelDataService.searchTravel(query);
    }


}
