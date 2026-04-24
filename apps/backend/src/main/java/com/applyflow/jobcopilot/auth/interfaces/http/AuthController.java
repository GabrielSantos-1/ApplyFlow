package com.applyflow.jobcopilot.auth.interfaces.http;

import com.applyflow.jobcopilot.auth.application.dto.request.LoginRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.LogoutRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.RefreshTokenRequest;
import com.applyflow.jobcopilot.auth.application.dto.response.AuthTokensResponse;
import com.applyflow.jobcopilot.auth.application.dto.response.CurrentUserResponse;
import com.applyflow.jobcopilot.auth.application.usecase.AuthUseCase;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthTokensResponse auth = authUseCase.login(request);
        setRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(maskRefresh(auth));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                      @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
                                                      HttpServletResponse response) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : cookieRefreshToken;
        AuthTokensResponse auth = authUseCase.refresh(new RefreshTokenRequest(token));
        setRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(maskRefresh(auth));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest request,
                       @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
                       HttpServletResponse response) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : cookieRefreshToken;
        authUseCase.logout(new LogoutRequest(token));
        clearRefreshCookie(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok(authUseCase.me());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private AuthTokensResponse maskRefresh(AuthTokensResponse source) {
        return new AuthTokensResponse(
                source.accessToken(),
                source.tokenType(),
                source.expiresInSeconds(),
                source.userId(),
                source.email(),
                source.role(),
                null
        );
    }
}
