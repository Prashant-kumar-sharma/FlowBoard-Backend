package com.flowboard.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "notificationCardLookupClient", url = "${card.service.base-url}")
public interface CardLookupClient {

    @GetMapping("/{cardId}")
    Map<String, Object> getCardById(@PathVariable String cardId);
}
