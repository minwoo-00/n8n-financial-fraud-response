package com.fds.controller;

import com.fds.dto.TransferRequest;
import com.fds.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/api/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestParam String userId,
            @RequestParam Long amount,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "false") Boolean verified,
            HttpServletRequest request
    ) {
        log.info("=== TRANSFER REQUEST START ===");
        log.info("userId: {}", userId);
        log.info("amount: {}", amount);
        log.info("country: {}", country);
        log.info("verified: {}", verified);

        // 당일 평균 계산
        double avgAmount = transferService.getTodayAverageAmount(userId);
        log.info("Today's average amount for {}: {}", userId, avgAmount);

        Map<String, Object> result = transferService.processTransfer(userId, amount, country, verified, request, avgAmount);

        log.info("=== TRANSFER RESULT ===");
        log.info("result: {}", result);

        return ResponseEntity.ok(result);
    }

    // 기존 /transfer 엔드포인트
    @PostMapping("/transfer")
    public String transferLegacy(
            @RequestBody TransferRequest req,
            HttpServletRequest httpRequest
    ) {
        log.info("=== LEGACY TRANSFER REQUEST ===");
        log.info("userId: {}", req.userId());
        log.info("amount: {}", req.amount());
        log.info("country: {}", req.country());

        // 당일 평균 계산
        double avgAmount = transferService.getTodayAverageAmount(req.userId());
        log.info("Today's average amount for {}: {}", req.userId(), avgAmount);

        Map<String, Object> result = transferService.processTransfer(
                req.userId(),
                req.amount(),
                req.country(),
                false,
                httpRequest,
                avgAmount
        );

        log.info("=== LEGACY TRANSFER RESULT ===");
        log.info("result: {}", result);

        return "TRANSFER_REQUESTED";
    }
}