package com.finflow.authservice.dto;

import com.finflow.authservice.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private Role role;
}
