package com.tinylight.service;

import com.tinylight.dto.LightRequest;
import com.tinylight.dto.LightResponse;
import com.tinylight.entity.TinyLight;
import com.tinylight.mapper.TinyLightMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class TinyLightService {
    private final TinyLightMapper mapper;
    public TinyLightService(TinyLightMapper mapper) {
        this.mapper = mapper;
    }

    public LightResponse create(LightRequest req) {
        LocalDate today = LocalDate.now();
        if (mapper.countByUserIdAndDate(req.getUserId(), today) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "今天已经点亮啦");
        }
        TinyLight t = new TinyLight();
        t.setUserId(req.getUserId());
        t.setContent(req.getContent());
        t.setMood(req.getMood());
        t.setLightDate(today);
        t.setCreatedAt(java.time.LocalDateTime.now());
        mapper.insert(t);
        return toResponse(t);
    }

    public LightResponse getToday(String userId) {
        TinyLight t = mapper.selectByUserIdAndDate(userId, LocalDate.now());
        return t == null ? null : toResponse(t);
    }

    public List<LightResponse> getRiver(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return mapper.selectByUserIdAndDateRange(userId, start, end).stream()
                .map(this::toResponse).toList();
    }

    public List<LightResponse> getOnThisDay(String userId) {
        LocalDate today = LocalDate.now();
        List<TinyLight> list = mapper.selectByUserIdAndDateRange(
                userId, today.minusYears(5), today.minusYears(1));
        return list.stream()
                .filter(t -> t.getLightDate().getMonth() == today.getMonth()
                        && t.getLightDate().getDayOfMonth() == today.getDayOfMonth())
                .map(this::toResponse).toList();
    }

    public LightResponse getById(Long id) {
        TinyLight t = mapper.selectById(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到这颗星");
        return toResponse(t);
    }

    private LightResponse toResponse(TinyLight t) {
        return LightResponse.builder()
                .id(t.getId()).userId(t.getUserId()).content(t.getContent())
                .mood(t.getMood()).lightDate(t.getLightDate()).createdAt(t.getCreatedAt())
                .build();
    }
}
