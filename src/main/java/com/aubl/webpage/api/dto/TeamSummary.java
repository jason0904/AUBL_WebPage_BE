package com.aubl.webpage.api.dto;

public record TeamSummary(
    Long id,
    String teamName,
    String teamCode,
    boolean active
) {
}
