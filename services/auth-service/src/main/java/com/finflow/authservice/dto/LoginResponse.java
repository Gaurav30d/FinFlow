package com.finflow.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
}
//{
//  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
//  "tokenType": "Bearer"
//}