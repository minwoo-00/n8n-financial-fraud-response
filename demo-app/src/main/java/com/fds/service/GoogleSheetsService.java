package com.fds.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.user-risk-range}")
    private String userRiskRange;

    //Google Sheets에서 특정 사용자의 Current_Total_Score 조회
    public Integer getCurrentTotalScore(String userId) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, userRiskRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.warn("No data found in Google Sheets");
                return 0;
            }

            // 헤더 행 건너뛰기 (1행부터 시작)
            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);

                // A열: User_ID, B열: Current_Total_Score
                if (row.size() >= 2) {
                    String sheetUserId = row.get(0).toString();

                    if (userId.equals(sheetUserId)) {
                        try {
                            Object scoreObj = row.get(1);
                            int score = scoreObj instanceof Number
                                    ? ((Number) scoreObj).intValue()
                                    : Integer.parseInt(scoreObj.toString());

                            log.info("Found score for user {}: {}", userId, score);
                            return score;
                        } catch (NumberFormatException e) {
                            log.error("Invalid score format for user {}: {}", userId, row.get(1));
                            return 0;
                        }
                    }
                }
            }

            log.warn("User {} not found in Google Sheets", userId);
            return 0;

        } catch (IOException e) {
            log.error("Error reading from Google Sheets for user {}", userId, e);
            return 0;
        }
    }

    //사용자의 blocked 상태 조회 E열(index 4)에 blocked 값이 있음

    public boolean isUserBlocked(String userId) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, userRiskRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.warn("No data found in Google Sheets for blocked check");
                return false;
            }

            // 헤더를 제외하고 user_id와 일치하는 행 찾기
            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.size() > 0 && userId.equals(row.get(0).toString())) {
                    // blocked 컬럼(E열 = index 4)이 있고 TRUE면 차단
                    if (row.size() > 4) {
                        String blockedValue = row.get(4).toString().trim().toUpperCase();
                        boolean isBlocked = "TRUE".equals(blockedValue);
                        log.info("User {} blocked status from sheets: {}", userId, isBlocked);
                        return isBlocked;
                    }
                    log.info("User {} has no blocked value in sheets, defaulting to false", userId);
                    return false;
                }
            }

            log.warn("User {} not found in Google Sheets", userId);
            return false;
        } catch (Exception e) {
            log.error("Failed to get blocked status for user: {}", userId, e);
            return false; // 에러 시 기본값
        }
    }

    //사용자를 차단 상태로 변경 E열에 blocked 값을 TRUE로 설정
    public void blockUser(String userId) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, userRiskRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.warn("No data found in Google Sheets");
                return;
            }

            // user_id와 일치하는 행 찾기
            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.size() > 0 && userId.equals(row.get(0).toString())) {
                    // blocked 컬럼 업데이트 (E열)
                    int rowNumber = i + 1; // 1-based index
                    String sheetName = userRiskRange.split("!")[0];
                    String updateRange = sheetName + "!E" + rowNumber;

                    ValueRange body = new ValueRange()
                            .setValues(List.of(List.of("TRUE")));

                    sheetsService.spreadsheets().values()
                            .update(spreadsheetId, updateRange, body)
                            .setValueInputOption("RAW")
                            .execute();

                    log.info("User {} blocked in Google Sheets at row {} (E column)", userId, rowNumber);
                    return;
                }
            }

            log.warn("User {} not found in Google Sheets for blocking", userId);
        } catch (Exception e) {
            log.error("Failed to block user: {}", userId, e);
        }
    }
}