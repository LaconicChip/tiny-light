package com.tinylight.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TinyLight {
    private Long id;
    private String userId;
    private String content;
    private String mood;
    private LocalDate lightDate;
    private LocalDateTime createdAt;
}
