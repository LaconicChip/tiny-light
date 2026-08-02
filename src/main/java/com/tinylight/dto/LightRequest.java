package com.tinylight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LightRequest {
    @NotBlank private String userId;
    @NotBlank private String content;
    @NotBlank
    @Pattern(regexp = "开心|平静|感恩|疲惫|感动|思念|期待", message = "心情不在允许范围内")
    private String mood;
}
