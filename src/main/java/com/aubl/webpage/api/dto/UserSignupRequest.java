package com.aubl.webpage.api.dto;

public record UserSignupRequest(
    String email,
    String password,
    String name,
    String phoneNumber
) {
}
