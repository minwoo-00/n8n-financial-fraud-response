package com.fds.service;

import com.fds.dto.FdsEvent;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSender {

    private final WebClient webClient;

    public void send(FdsEvent event) {

        webClient.post()
                .uri("webhook/dd866d46-8a0b-4dfc-b853-55d3179511fd")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("n8n webhook success"))
                .doOnError(err -> log.error("n8n webhook error", err))
                .subscribe(); // 비동기 전송
    }
}
