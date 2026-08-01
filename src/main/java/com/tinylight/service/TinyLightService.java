package com.tinylight.service;

import com.tinylight.dto.LightRequest;
import com.tinylight.dto.LightResponse;
import com.tinylight.dto.PageResponse;
import com.tinylight.dto.StatsResponse;
import com.tinylight.entity.TinyLight;
import com.tinylight.mapper.TinyLightMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

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
        try {
            mapper.insert(t);
        } catch (DuplicateKeyException e) {
            // 并发兜底：count 检查和 insert 之间另一个请求已写入
            throw new ResponseStatusException(HttpStatus.CONFLICT, "今天已经点亮啦");
        }
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

    public LightResponse getById(Long id, String userId) {
        TinyLight t = mapper.selectById(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到这颗星");
        if (!t.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看他人的微光");
        }
        return toResponse(t);
    }

    public LightResponse update(Long id, LightRequest req) {
        TinyLight t = mapper.selectById(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到这颗星");
        if (!t.getUserId().equals(req.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改他人的微光");
        }
        if (!t.getLightDate().equals(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能编辑今天的微光");
        }
        t.setContent(req.getContent());
        t.setMood(req.getMood());
        mapper.update(t);
        return toResponse(t);
    }

    public void delete(Long id, String userId) {
        TinyLight t = mapper.selectById(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到这颗星");
        if (!t.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除他人的微光");
        }
        if (!t.getLightDate().equals(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能删除今天的微光");
        }
        mapper.deleteById(id);
    }

    public StatsResponse getStats(String userId) {
        int totalDays = mapper.countAllByUserId(userId);
        int yearDays = mapper.countByUserIdAndYear(userId, LocalDate.now().getYear());

        List<LocalDate> dates = mapper.selectAllDatesByUserId(userId);
        int currentStreak = computeCurrentStreak(dates);
        int longestStreak = computeLongestStreak(dates);

        Map<String, Integer> moodDist = new LinkedHashMap<>();
        for (Map<String, Object> row : mapper.selectMoodDistribution(userId)) {
            String mood = (String) row.get("mood");
            Number cnt = (Number) row.get("cnt");
            moodDist.put(mood, cnt.intValue());
        }

        return StatsResponse.builder()
                .totalDays(totalDays)
                .yearDays(yearDays)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .moodDistribution(moodDist)
                .build();
    }

    public PageResponse<LightResponse> list(String userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        int offset = (page - 1) * size;
        int total = mapper.countAllByUserId(userId);
        List<LightResponse> items = mapper.selectByUserIdPaged(userId, offset, size).stream()
                .map(this::toResponse).toList();
        return PageResponse.<LightResponse>builder()
                .page(page).size(size).total(total).items(items)
                .build();
    }

    // 今天有记录从今天数；今天没但昨天有从昨天数（今天还没过完）；否则 0
    private int computeCurrentStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        Set<LocalDate> set = new HashSet<>(dates);
        LocalDate today = LocalDate.now();
        LocalDate start = set.contains(today) ? today
                : (set.contains(today.minusDays(1)) ? today.minusDays(1) : null);
        if (start == null) return 0;
        int streak = 0;
        for (LocalDate d = start; set.contains(d); d = d.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    private int computeLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);
        int longest = 1, current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    private LightResponse toResponse(TinyLight t) {
        return LightResponse.builder()
                .id(t.getId()).userId(t.getUserId()).content(t.getContent())
                .mood(t.getMood()).lightDate(t.getLightDate()).createdAt(t.getCreatedAt())
                .build();
    }
}
