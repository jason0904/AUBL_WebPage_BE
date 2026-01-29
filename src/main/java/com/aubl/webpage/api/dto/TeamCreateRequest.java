package com.aubl.webpage.api.dto;

public record TeamCreateRequest(
    String teamName,
    String teamCode,
    Long managerId
) {
}
