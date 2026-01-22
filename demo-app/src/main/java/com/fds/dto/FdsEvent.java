package com.fds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FdsEvent(
        @JsonProperty("ts")
        String ts,
        @JsonProperty("event_type")
        String eventType,
        @JsonProperty("event_id")
        String eventId,
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("result")
        String result,
        @JsonProperty("src_ip")
        String srcIp,
        @JsonProperty("country")
        String country,
        @JsonProperty("hour")
        Integer hour,
        @JsonProperty("amount")
        Long amount,
        @JsonProperty("to_bank")
        String toBank,
        @JsonProperty("to_account_id")
        String toAccountId,
        @JsonProperty("avgAmount")
        double avgAmount
) {}

