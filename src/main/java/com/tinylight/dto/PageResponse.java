package com.tinylight.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private int page;
    private int size;
    private int total;
    private List<T> items;
}
