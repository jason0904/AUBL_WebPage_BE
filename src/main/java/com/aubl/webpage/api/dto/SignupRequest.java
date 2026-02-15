package com.aubl.webpage.api.dto;

public record SignupRequest(
    String email,
    String password,
    String name,
    String phoneNumber
) {
}

