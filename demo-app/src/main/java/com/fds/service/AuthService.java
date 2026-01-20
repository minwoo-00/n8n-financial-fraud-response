package com.fds.service;

import com.fds.dto.FdsEvent;
import com.fds.dto.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";

    private static final Map<String, User> USERS = new ConcurrentHashMap<>();

    static{
        USERS.put("user_01", new User("user_01", "12345678", "NORMAL"));
        USERS.put("user_02", new User("user_02", "12341234", "NORMAL"));
    }

    private static final Map<String, String> COUNTRY_IP_MAP = Map.of(
            "KR", "203.0.113.10",
            "US", "198.51.100.23",
            "JP", "192.0.2.44",
            "SG", "203.0.113.77",
            "GB", "198.51.100.88"
    );

    private final EventSender eventSender;

    public String login(String userId, String password, String country, HttpServletRequest request) {
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);
        ZonedDateTime now = ZonedDateTime.now();

        //사용자 조회
        User user = USERS.get(userId);
        if (user == null) {
            log.warn("LOGIN_FAILURE userId={} reason=USER_NOT_FOUND", userId);
            FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, RESULT_FAILURE);
            eventSender.send(event);
            return RESULT_FAILURE;
        }

        //차단된 계정 체크
        if ("BLOCKED".equals(user.getStatus())) {
            log.warn("LOGIN_BLOCKED user_Id={} country={} srcIp={} status=BLOCKED",
                        userId, normalizedCountry, srcIp);
            FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, "BLOCKED");
            eventSender.send(event);
            return "BLOCKED";
        }

        //비밀번호 검증
        if (!user.getPassword().equals(password)) {
            log.warn("LOGIN_FAILURE userId={} reason=INVALID_PASSWORD", userId);
            FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, RESULT_FAILURE);
            eventSender.send(event);
            return RESULT_FAILURE;
        }

        //MID 상태 체크
        if ("MEDIUM".equals(user.getStatus())) {
            log.warn("LOGIN_MID_VERIFICATION userId={} country={} srcIp={} status=MID", userId, normalizedCountry, srcIp);
            FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, "MID_VERIFICATION");
            eventSender.send(event);
            return "MID_VERIFICATION";
        }

        //로그인 성공
        log.info("LOGIN_SUCCESS userId={} country={} srcIp={} timestamp={} status={}",
                userId, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), user.getStatus());

        FdsEvent event = createAuthEvent("LOGIN", userId, normalizedCountry, srcIp, RESULT_SUCCESS);
        eventSender.send(event);

        return RESULT_SUCCESS;
    }

    public String logout(String userId, String country, HttpServletRequest request) {
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);
        ZonedDateTime now = ZonedDateTime.now();

        //사용자 존재 확인
        if (!USERS.containsKey(userId)) {
            log.warn("LOGOUT_FAILURE userId={} reason=USER_NOT_FOUND", userId);
            return RESULT_FAILURE;
        }

        //FDS 이벤트 전송
        FdsEvent event = createAuthEvent("LOGOUT", userId, normalizedCountry, srcIp, RESULT_SUCCESS);
        eventSender.send(event);

        //ELK 로그 기록
        log.info("LOGOUT_SUCCESS userId={} country={} srcIp={} timestamp={}",
                        userId, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return RESULT_SUCCESS;
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
                null
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

    //user 상태 업데이트
    public static void updateUserStatus(String userId, String status) {
        User user = USERS.get(userId);
        if (user != null) {
            user.setStatus(status);
        }
    }

    public static String getUserStatus(String userId) {
        User user = USERS.get(userId);
        return user != null ? user.getStatus() : "UNKNOWN";
    }

}

