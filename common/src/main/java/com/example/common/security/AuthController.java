package com.example.common.security;

import com.example.common.dto.Result;
import com.example.common.enums.ErrorCode;
import com.example.common.security.dto.AuthResponse;
import com.example.common.security.dto.LoginRequest;
import com.example.common.security.dto.RefreshTokenRequest;
import com.example.common.security.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 提供登录、注册、刷新令牌、查询当前用户、登出接口。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造认证控制器。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 认证响应
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 刷新访问令牌。
     *
     * @param request 刷新令牌请求
     * @return 认证响应
     */
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request));
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return Result.fail(ErrorCode.UNAUTHORIZED);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("roles", user.getRoles());
        return Result.success(data);
    }

    /**
     * 用户登出。
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
