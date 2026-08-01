package com.tinylight.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class StatsResponse {
    private int totalDays;
    private int yearDays;
    private int currentStreak;
    private int longestStreak;
    private Map<String, Integer> moodDistribution;
}
