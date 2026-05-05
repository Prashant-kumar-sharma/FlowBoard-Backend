package com.flowboard.auth.controller;

import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    @Mock
    private AuthService authService;

    private InternalUserController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalUserController(authService);
    }

    @Test
    void getUserByIdDelegatesToService() {
        UserResponse response = UserResponse.builder().id(1L).build();
        when(authService.getUserById(1L)).thenReturn(response);

        assertThat(controller.getUserById(1L).getBody()).isEqualTo(response);
    }

    @Test
    void getUserByUsernameDelegatesToService() {
        UserResponse response = UserResponse.builder().username("alice").build();
        when(authService.getUserByUsername("alice")).thenReturn(response);

        assertThat(controller.getUserByUsername("alice").getBody()).isEqualTo(response);
    }
}
