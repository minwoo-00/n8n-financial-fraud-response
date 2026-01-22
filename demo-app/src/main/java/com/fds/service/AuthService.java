package com.fds.service;

import com.fds.dto.FdsEvent;
import com.fds.dto.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger elkLog = LoggerFactory.getLogger("ELK_LOGIN");

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";

    private static final Map<String, User> USERS = new ConcurrentHashMap<>();

    static {
        USERS.put("user_01", new User("user_01", "12345678", false));
        USERS.put("user_02", new User("user_02", "12341234", false));
    }

    private static final Map<String, String> COUNTRY_IP_MAP = Map.of(
            "KR", "203.0.113.10",
            "US", "198.51.100.23",
            "JP", "192.0.2.44",
            "SG", "203.0.113.77",
            "GB", "198.51.100.88"
    );

    private final EventSender eventSender;
    private final GoogleSheetsService googleSheetsService;

    public String login(String userId, String password, String country, HttpServletRequest request) {
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);
        ZonedDateTime now = ZonedDateTime.now();

        // 사용자 조회
        User user = USERS.get(userId);
        if (user == null) {
            log.warn("LOGIN_FAILURE userId={} reason=USER_NOT_FOUND", userId);
            return RESULT_FAILURE;
        }

        // 비밀번호 검증
        if (!user.getPassword().equals(password)) {
            log.warn("LOGIN_FAILURE userId={} reason=INVALID_PASSWORD", userId);
            return RESULT_FAILURE;
        }

        // 1. Google Sheets의 blocked 상태 체크
        boolean isBlockedInSheets = googleSheetsService.isUserBlocked(userId);
        if (isBlockedInSheets) {
            log.warn("LOGIN_BLOCKED userId={} country={} srcIp={} blocked=true (from Google Sheets)",
                    userId, normalizedCountry, srcIp);
            return "BLOCKED";
        }

        // 2. Risk Level 조회 (실시간 위험도 체크)
        String riskLevel = getRiskLevel(userId);

        // HIGH: Google Sheets에 blocked=TRUE 설정
        if ("HIGH".equals(riskLevel)) {
            log.warn("LOGIN_AUTO_BLOCKED userId={} country={} srcIp={} riskLevel=HIGH (auto-blocking)",
                    userId, normalizedCountry, srcIp);

            // Google Sheets에 blocked 설정
            googleSheetsService.blockUser(userId);

            return "BLOCKED";
        }

        // MEDIUM: 로그인 허용하되 경고 로그만 남김
        if ("MEDIUM".equals(riskLevel)) {
            log.warn("LOGIN_SUCCESS_WITH_MEDIUM_RISK userId={} country={} srcIp={} riskLevel=MEDIUM",
                    userId, normalizedCountry, srcIp);
        }

        // 로그인 성공 시에만 ELK 로그
        try {
            MDC.put("eventType", "LOGIN");
            MDC.put("userId", userId);
            MDC.put("country", normalizedCountry);
            MDC.put("srcIp", srcIp);
            MDC.put("riskLevel", riskLevel);
            MDC.put("timestamp", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            elkLog.info("LOGIN_SUCCESS");
        } finally {
            MDC.clear();
        }

        FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, RESULT_SUCCESS);
        eventSender.send(event);

        return RESULT_SUCCESS;
    }

    public String logout(String userId, String country, HttpServletRequest request) {
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);
        ZonedDateTime now = ZonedDateTime.now();

        // 사용자 존재 확인
        if (!USERS.containsKey(userId)) {
            log.warn("LOGOUT_FAILURE userId={} reason=USER_NOT_FOUND", userId);
            return RESULT_FAILURE;
        }

        // FDS 이벤트 전송
        FdsEvent event = createAuthEvent("LOGOUT", userId, normalizedCountry, srcIp, RESULT_SUCCESS);
        eventSender.send(event);

        // ELK 로그 기록
        log.info("LOGOUT_SUCCESS userId={} country={} srcIp={} timestamp={}",
                userId, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return RESULT_SUCCESS;
    }

    // Google Sheets에서 Current_Total_Score를 조회하여 Risk Level 계산
    private String getRiskLevel(String userId) {
        try {
            // Google Sheets API로 Current_Total_Score 조회
            int score = googleSheetsService.getCurrentTotalScore(userId);

            if (score >= 70) {
                return "HIGH";
            } else if (score >= 40) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        } catch (Exception e) {
            log.error("Failed to get risk level for user: {}", userId, e);
            return "LOW";
        }
    }

    private FdsEvent createAuthEvent(String eventType, String userId, String country, String srcIp, String result) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        return new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                eventType,
                UUID.randomUUID().toString(),
                userId,
                result,
                srcIp,
                country,
                now.getHour(),
                null,
                null,
                null,
                0.0
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

    public static User getUser(String userId) {
        return USERS.get(userId);
    }
}