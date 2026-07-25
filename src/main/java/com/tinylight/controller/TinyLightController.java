package com.tinylight.controller;

import com.tinylight.dto.LightRequest;
import com.tinylight.dto.LightResponse;
import com.tinylight.service.TinyLightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lights")
public class TinyLightController {
    private final TinyLightService service;
    public TinyLightController(TinyLightService service) { this.service = service; }

    @PostMapping
    public LightResponse create(@RequestBody @Valid LightRequest req) { return service.create(req); }

    @GetMapping("/today")
    public Map<String, Object> today(@RequestParam String userId) {
        LightResponse r = service.getToday(userId);
        return Map.of("todayLighted", r != null, "light", r);
    }

    @GetMapping("/river")
    public List<LightResponse> river(@RequestParam String userId, @RequestParam int year) {
        return service.getRiver(userId, year);
    }

    @GetMapping("/on-this-day")
    public List<LightResponse> onThisDay(@RequestParam String userId) {
        return service.getOnThisDay(userId);
    }

    @GetMapping("/{id}")
    public LightResponse detail(@PathVariable Long id) { return service.getById(id); }
}
