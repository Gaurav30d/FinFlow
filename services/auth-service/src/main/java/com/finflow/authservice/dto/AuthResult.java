package com.finflow.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResult {

    private String accessToken;
    private String refreshToken;
}