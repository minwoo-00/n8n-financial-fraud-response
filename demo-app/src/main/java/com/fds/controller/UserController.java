package com.fds.controller;

import com.fds.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    // n8n에서 위험도 분석 결과 받기
    @PostMapping("/update-risk")
    public Map<String, String> updateRiskStatus(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        String riskLevel = request.get("risk_level"); // "LOW", "MEDIUM", "HIGH"

        String newStatus;

        switch (riskLevel.toUpperCase()) {
            case "HIGH":
                newStatus = "BLOCKED";
                AuthService.updateUserStatus(userId, newStatus);
                log.warn("RISK_UPDATE userId={} riskLevel={} newStatus=BLOCKED", userId, riskLevel);
                break;

            case "MEDIUM":
                newStatus = "MEDIUM";
                AuthService.updateUserStatus(userId, newStatus);
                log.warn("RISK_UPDATE userId={} riskLevel={} newStatus=MEDIUM", userId, riskLevel);
                break;

            case "LOW":
            case "NORMAL":
            default:
                newStatus = "NORMAL";
                AuthService.updateUserStatus(userId, newStatus);
                log.info("RISK_UPDATE userId={} riskLevel={} newStatus=NORMAL", userId, riskLevel);
                break;
        }

        return Map.of(
                "status", "success",
                "user_id", userId,
                "risk_level", riskLevel,
                "new_status", newStatus
        );
    }

    @PostMapping("/block")
    public Map<String, String> blockUser(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");

        AuthService.updateUserStatus(userId, "BLOCKED");
        log.warn("USER_BLOCKED userId={}", userId);

        return Map.of(
                "status", "success",
                "message", "User blocked: " + userId
        );
    }

    @PostMapping("/unblock")
    public Map<String, String> unblockUser(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");

        AuthService.updateUserStatus(userId, "NORMAL");
        log.info("USER_UNBLOCKED userId={}", userId);

        return Map.of(
                "status", "success",
                "message", "User unblocked: " + userId
        );
    }

    @PostMapping("/set-mid")
    public Map<String, String> setMidStatus(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");

        AuthService.updateUserStatus(userId, "MEDIUM");
        log.warn("USER_MEDIUM_STATUS userId={}", userId);

        return Map.of(
                "status", "success",
                "message", "User set to MEDIUM status: " + userId
        );
    }

    @GetMapping("/status/{userId}")
    public Map<String, Object> getUserStatus(@PathVariable String userId) {
        String status = AuthService.getUserStatus(userId);

        return Map.of(
                "userId", userId,
                "status", status
        );
    }
}