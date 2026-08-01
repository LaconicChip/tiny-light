package com.tinylight.controller;

import com.tinylight.dto.LightRequest;
import com.tinylight.dto.LightResponse;
import com.tinylight.dto.PageResponse;
import com.tinylight.dto.StatsResponse;
import com.tinylight.service.TinyLightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lights")
public class TinyLightController {
    private final TinyLightService service;
    public TinyLightController(TinyLightService service) { this.service = service; }

    @PostMapping
    public LightResponse create(@RequestBody @Valid LightRequest req) { return service.create(req); }

    @GetMapping
    public PageResponse<LightResponse> list(@RequestParam String userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.list(userId, page, size);
    }

    @GetMapping("/today")
    public Map<String, Object> today(@RequestParam String userId) {
        LightResponse r = service.getToday(userId);
        // 不能用 Map.of：今天没记录时 r 为 null，Map.of 不允许 null 值会抛 NPE
        Map<String, Object> result = new HashMap<>();
        result.put("todayLighted", r != null);
        result.put("light", r);
        return result;
    }

    @GetMapping("/river")
    public List<LightResponse> river(@RequestParam String userId, @RequestParam int year) {
        return service.getRiver(userId, year);
    }

    @GetMapping("/on-this-day")
    public List<LightResponse> onThisDay(@RequestParam String userId) {
        return service.getOnThisDay(userId);
    }

    @GetMapping("/stats")
    public StatsResponse stats(@RequestParam String userId) {
        return service.getStats(userId);
    }

    @GetMapping("/{id}")
    public LightResponse detail(@PathVariable Long id, @RequestParam String userId) {
        return service.getById(id, userId);
    }

    @PutMapping("/{id}")
    public LightResponse update(@PathVariable Long id, @RequestBody @Valid LightRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam String userId) {
        service.delete(id, userId);
    }
}
