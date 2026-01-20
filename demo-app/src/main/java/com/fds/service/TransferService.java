package com.fds.service;

import com.fds.dto.FdsEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final String RESULT_SUCCESS = "SUCCESS";
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

    public void transfer(String userId, Long amount, String country, HttpServletRequest request) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);
        String userStatus = AuthService.getUserStatus(userId);

        //Redis 카운트 증가 로직
        String redisKey = "tx_count:" + userId;
        Long txCount = redisTemplate.opsForValue().increment(redisKey);

        if (txCount != null && txCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(10));
            log.info("Redis key created: {} with TTL 10 minutes", redisKey);
        }
        log.info("User {} transfer count in 10min: {}", userId, txCount);


        if ("BLOCKED".equals(userStatus)) {
            log.warn("TRANSFER_BLOCKED userId={} amount={} reason=ACCOUNT_BLOCKED", userId, amount);

            FdsEvent event = new FdsEvent(
                    now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "TRANSFER",
                    UUID.randomUUID().toString(),
                    userId,
                    "BLOCKED",
                    srcIp,
                    normalizedCountry,
                    now.getHour(),
                    amount,
                    SAMPLE_TO_BANK,
                    SAMPLE_TO_ACCOUNT_ID
            );
            eventSender.send(event);

            throw new IllegalStateException("의심스러운 활동으로 인해 계정이 차단되었습니다.");
        }

        if ("MEDIUM".equals(userStatus)) {
            log.warn("TRANSFER_MEDIUM_VERIFICATION userId={} amount={} status=MEDIUM", userId, amount);

            FdsEvent event = new FdsEvent(
                    now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "TRANSFER",
                    UUID.randomUUID().toString(),
                    userId,
                    "MID_VERIFICATION",
                    srcIp,
                    normalizedCountry,
                    now.getHour(),
                    amount,
                    SAMPLE_TO_BANK,
                    SAMPLE_TO_ACCOUNT_ID
            );
            eventSender.send(event);

            throw new IllegalStateException("보안 확인이 필요합니다.");
        }

        FdsEvent event = new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "TRANSFER",
                UUID.randomUUID().toString(),
                userId,
                RESULT_SUCCESS,
                srcIp,
                normalizedCountry,
                now.getHour(),
                amount,
                SAMPLE_TO_BANK,
                SAMPLE_TO_ACCOUNT_ID
        );

        eventSender.send(event);

        //ELK 로그
        log.info("TRANSFER_SUCCESS userId={} amount={} country={} srcIp={} timestamp={} toBank={} toAccountId={} status={}",
                    userId, amount, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), SAMPLE_TO_BANK, SAMPLE_TO_ACCOUNT_ID, userStatus);
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

