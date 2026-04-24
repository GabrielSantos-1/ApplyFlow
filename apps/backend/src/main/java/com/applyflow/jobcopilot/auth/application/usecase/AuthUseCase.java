package com.applyflow.jobcopilot.auth.application.usecase;

import com.applyflow.jobcopilot.auth.application.dto.request.LoginRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.LogoutRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.RefreshTokenRequest;
import com.applyflow.jobcopilot.auth.application.dto.response.AuthTokensResponse;
import com.applyflow.jobcopilot.auth.application.dto.response.CurrentUserResponse;

public interface AuthUseCase {
    AuthTokensResponse login(LoginRequest request);
    AuthTokensResponse refresh(RefreshTokenRequest request);
    void logout(LogoutRequest request);
    CurrentUserResponse me();
}
