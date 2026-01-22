package com.fds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fds.dto.FdsEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger elkLog = LoggerFactory.getLogger("ELK_LOGIN");

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_VERIFICATION_REQUIRED = "VERIFICATION_REQUIRED";
    private static final String RESULT_FORCE_LOGOUT = "FORCE_LOGOUT";
    private static final String SAMPLE_TO_BANK = "Woori";
    private static final String SAMPLE_TO_ACCOUNT_ID = "110-***-1234";
    private static final Map<String, String> COUNTRY_IP_MAP = Map.of(
            "KR", "203.0.113.10",
            "US", "198.51.100.23",
            "JP", "192.0.2.44",
            "SG", "203.0.113.77",
            "GB", "198.51.100.88"
    );

    private final EventSender eventSender;
    private final StringRedisTemplate redisTemplate;
    private final GoogleSheetsService googleSheetsService;
    private final ObjectMapper objectMapper;
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public double getTodayAverageAmount(String userId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String logFileName = LOG_DIR + "/fds-" + today + ".json";

        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            log.warn("Log file not found: {}", logFileName);
            return getRecentAverageAmount(userId);
        }

        List<Double> amounts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode node = objectMapper.readTree(line);

                    if ("TRANSFER".equals(node.path("eventType").asText()) &&
                            userId.equals(node.path("userId").asText())) {

                        String amountStr = node.path("amount").asText();
                        log.info("Found amount string: {}", amountStr);
                        if (!amountStr.isEmpty()) {
                            double amount = Double.parseDouble(amountStr);
                            log.info("Parsed amount: {}", amount);
                            amounts.add(amount);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse log line: {}", line);
                }
            }
        } catch (Exception e) {
            log.error("Error reading log file: {}", logFileName, e);
            return getRecentAverageAmount(userId);
        }

        if (amounts.isEmpty()) {
            log.info("No transfer records found for user {} on {}, checking recent days", userId, today);
            return getRecentAverageAmount(userId);
        }

        log.info("All amounts for {}: {}", userId, amounts);

        double average = amounts.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        log.info("User {} today's average transfer amount: {} (based on {} transfers)",
                userId, average, amounts.size());

        return average;
    }

    private double getRecentAverageAmount(String userId) {
        // 최근 7일 동안 데이터 확인
        for (int daysAgo = 1; daysAgo <= 7; daysAgo++) {
            String targetDate = LocalDate.now().minusDays(daysAgo).format(DATE_FORMATTER);
            String logFileName = LOG_DIR + "/fds-" + targetDate + ".json";

            File logFile = new File(logFileName);
            if (!logFile.exists()) {
                log.debug("Log file not found for {} days ago: {}", daysAgo, logFileName);
                continue;
            }

            List<Double> amounts = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JsonNode node = objectMapper.readTree(line);

                        if ("TRANSFER".equals(node.path("eventType").asText()) &&
                                userId.equals(node.path("userId").asText())) {

                            String amountStr = node.path("amount").asText();
                            if (!amountStr.isEmpty()) {
                                double amount = Double.parseDouble(amountStr);
                                amounts.add(amount);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse log line: {}", line);
                    }
                }
            } catch (Exception e) {
                log.error("Error reading log file: {}", logFileName, e);
                continue;
            }

            if (!amounts.isEmpty()) {
                double average = amounts.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                log.info("User {} recent average transfer amount from {} ({} days ago): {} (based on {} transfers)",
                        userId, targetDate, daysAgo, average, amounts.size());

                return average;
            }
        }

        log.info("No transfer records found for user {} in recent 7 days", userId);
        return 0.0;
    }

    public Map<String, Object> processTransfer(String userId, Long amount, String country, Boolean verified, HttpServletRequest request, double avgAmount) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);

        // Redis 카운트 증가
        incrementTransferCount(userId);

        // 1. Blocked 상태 체크
        if (googleSheetsService.isUserBlocked(userId)) {
            log.warn("TRANSFER_BLOCKED userId={} amount={} reason=BLOCKED_IN_SHEETS", userId, amount);
            return createForceLogoutResponse(amount, "계정이 차단되었습니다.");
        }

        // 2. Risk Level 체크 및 처리
        String riskLevel = evaluateRiskLevel(userId, verified, amount);

        if (RESULT_FORCE_LOGOUT.equals(riskLevel)) {
            return createForceLogoutResponse(amount, "의심스러운 활동이 감지되어 자동 로그아웃됩니다.");
        }

        if (RESULT_VERIFICATION_REQUIRED.equals(riskLevel)) {
            return createVerificationRequiredResponse(amount);
        }

        // 3. 정상 처리
        sendTransferEvent(userId, amount, normalizedCountry, srcIp, now, avgAmount);

        // 송금 성공 시에만 ELK 로그
        try {
            MDC.put("eventType", "TRANSFER");
            MDC.put("userId", userId);
            MDC.put("amount", String.valueOf(amount));
            MDC.put("country", normalizedCountry);
            MDC.put("srcIp", srcIp);
            MDC.put("riskLevel", riskLevel);
            MDC.put("toBank", SAMPLE_TO_BANK);
            MDC.put("timestamp", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            elkLog.info("TRANSFER_SUCCESS");
        } finally {
            MDC.clear();
        }

        return Map.of(
                "status", RESULT_SUCCESS,
                "message", "송금이 성공적으로 처리되었습니다.",
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    private void incrementTransferCount(String userId) {
        String redisKey = "tx_count:" + userId;
        Long txCount = redisTemplate.opsForValue().increment(redisKey);

        if (txCount != null && txCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(10));
            log.info("Redis key created: {} with TTL 10 minutes", redisKey);
        }
        log.info("User {} transfer count in 10min: {}", userId, txCount);
    }

    private String evaluateRiskLevel(String userId, Boolean verified, Long amount) {
        int score = googleSheetsService.getCurrentTotalScore(userId);
        String riskLevel = calculateRiskLevel(score);

        if (Boolean.TRUE.equals(verified)) {
            log.info("VERIFIED_TRANSFER userId={} amount={} score={} riskLevel={}", userId, amount, score, riskLevel);

            if ("HIGH".equals(riskLevel)) {
                log.warn("TRANSFER_AUTO_BLOCKED userId={} amount={} riskLevel=HIGH (auto-blocking on verified transfer)", userId, amount);
                googleSheetsService.blockUser(userId);
                return RESULT_FORCE_LOGOUT;
            }

            return "VERIFIED";
        }

        // verified=false인 경우
        if ("HIGH".equals(riskLevel)) {
            log.warn("TRANSFER_AUTO_BLOCKED userId={} amount={} riskLevel=HIGH (auto-blocking)", userId, amount);
            googleSheetsService.blockUser(userId);
            return RESULT_FORCE_LOGOUT;
        }

        if ("MEDIUM".equals(riskLevel)) {
            log.warn("TRANSFER_VERIFICATION_REQUIRED userId={} amount={} riskLevel=MEDIUM", userId, amount);
            return RESULT_VERIFICATION_REQUIRED;
        }

        return riskLevel;
    }

    private String calculateRiskLevel(int score) {
        if (score >= 70) {
            return "HIGH";
        } else if (score >= 40) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private void sendTransferEvent(String userId, Long amount, String country, String srcIp, ZonedDateTime now, double avgAmount) {
        FdsEvent event = new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "TRANSFER",
                UUID.randomUUID().toString(),
                userId,
                RESULT_SUCCESS,
                srcIp,
                country,
                now.getHour(),
                amount,
                SAMPLE_TO_BANK,
                SAMPLE_TO_ACCOUNT_ID,
                avgAmount
        );
        eventSender.send(event);
    }

    private Map<String, Object> createForceLogoutResponse(Long amount, String message) {
        return Map.of(
                "status", RESULT_FORCE_LOGOUT,
                "message", message,
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    private Map<String, Object> createVerificationRequiredResponse(Long amount) {
        return Map.of(
                "status", RESULT_VERIFICATION_REQUIRED,
                "message", "보안 확인이 필요합니다. 추가 인증을 완료해주세요.",
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return "UNKNOWN";
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String getClientIp(HttpServletRequest request, String country) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = COUNTRY_IP_MAP.getOrDefault(country, "203.0.113.200");
        }

        return ip;
    }
}