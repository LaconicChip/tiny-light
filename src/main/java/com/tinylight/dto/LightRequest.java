package com.tinylight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LightRequest {
    @NotBlank private String userId;
    @NotBlank private String content;
    @NotBlank private String mood;
}
