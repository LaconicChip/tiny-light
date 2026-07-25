package com.tinylight.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class LightResponse {
    private Long id;
    private String userId;
    private String content;
    private String mood;
    private LocalDate lightDate;
    private LocalDateTime createdAt;
}
