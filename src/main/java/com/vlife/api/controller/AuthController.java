package com.vlife.api.controller;

import com.vlife.api.service.AuthService;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.UserDao;
import com.vlife.shared.jdbc.entity.User;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

@Controller("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserDao userDao;

    @Inject
    public AuthController(AuthService authService, UserDao userDao) {
        this.authService = authService;
        this.userDao = userDao;
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<ApiResponse<LoginResponse>> login(@Body LoginRequest req) {
        String email = ApiUtil.trim(req.getEmail());
        String password = ApiUtil.trim(req.getPassword());

        if (CommonUtil.isNullOrEmpty(email)) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "email is required"));
        }

        if (CommonUtil.isNullOrEmpty(password)) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "password is required"));
        }

        return authService.login(email, password)
                .map(token -> HttpResponse.ok(
                        ApiResponse.success(new LoginResponse(token, "Bearer"))
                ))
                .orElseGet(() ->
                        HttpResponse.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(-401, "invalid email or password"))
                );
    }

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<ApiResponse<MeResponse>> me(Authentication authentication) {
        Long userId = ApiUtil.parseLong(authentication.getName());
        if (userId == null) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "invalid authentication"));
        }

        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return HttpResponse.notFound(ApiResponse.error(-404, "user not found"));
        }

        MeResponse data = new MeResponse();
        data.setId(user.getId());
        data.setEmail(user.getEmail());
        data.setName(user.getName());

        return HttpResponse.ok(ApiResponse.success(data));
    }

    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<ApiResponse<String>> logout() {
        return HttpResponse.ok(ApiResponse.success("Logged out"));
    }

    @Serdeable
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Serdeable
    public static class LoginResponse {
        private String accessToken;
        private String tokenType;

        public LoginResponse() {
        }

        public LoginResponse(String accessToken, String tokenType) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }

    @Serdeable
    public static class MeResponse {
        private Long id;
        private String email;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}