package com.fds.controller;

import com.fds.dto.LoginRequest;
import com.fds.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public String login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String result = authService.login(
                    request.userId(),
                    request.password(),
                    request.country(),
                    httpRequest
                );

        if (result.equals("BLOCKED")) {
            return "LOGIN_BLOCKED - 의심스러운 활동으로 인해 계정이 차단되었습니다.";
        }

        return result.equals("SUCCESS") ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
    }

    @PostMapping("/logout")
    public String logout(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String result = authService.logout(
                        request.userId(),
                        request.country(),
                        httpRequest
                    );

        return result.equals("SUCCESS") ? "LOGOUT_SUCCESS" : "LOGOUT_FAILURE";
    }

}

