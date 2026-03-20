package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String username;
    
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
    
    public String getUsernameOrEmail() {
        return username != null ? username : email;
    }
}
