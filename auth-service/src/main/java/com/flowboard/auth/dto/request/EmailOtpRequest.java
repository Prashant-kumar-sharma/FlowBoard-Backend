package com.flowboard.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailOtpRequest {
    @Email(message = "Email must be in a valid format, for example name@example.com")
    @NotBlank(message = "Email is required")
    private String email;
}
