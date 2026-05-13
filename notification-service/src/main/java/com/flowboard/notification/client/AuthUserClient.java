package com.flowboard.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "notificationAuthUserClient", url = "${auth.service.internal-base-url}")
public interface AuthUserClient {

    @GetMapping("/users/{userId}")
    Map<String, Object> getUserById(@PathVariable Long userId);

    @GetMapping("/users/username/{username}")
    Map<String, Object> getUserByUsername(@PathVariable String username);
}
