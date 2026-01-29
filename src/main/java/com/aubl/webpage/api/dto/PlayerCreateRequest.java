package com.aubl.webpage.api.dto;

import java.time.LocalDate;

public record PlayerCreateRequest(
    Long userId,
    String playerName,
    LocalDate birthDate,
    String position,
    Integer height,
    Integer weight,
    String school,
    Boolean isPlayer
) {
}
