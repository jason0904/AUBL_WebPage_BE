package com.aubl.webpage.api.dto;

import java.util.List;

public record RosterResponse(
    List<RosterItemResponse> items,
    String nextCursor,
    boolean hasNext,
    long totalCount
) {
}

