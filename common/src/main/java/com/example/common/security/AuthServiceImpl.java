package com.example.common.security;

import com.example.common.security.dto.AuthResponse;
import com.example.common.security.dto.LoginRequest;
import com.example.common.security.dto.RefreshTokenRequest;
import com.example.common.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    public AuthResponse login(LoginRequest request) {
        // 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        // 生成Token
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());
        
        // 获取用户信息
        User user = (User) authentication.getPrincipal();
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .username(user.getUsername())
                .role(user.getRoles().iterator().next().name())
                .build();
    }
    
    @Override
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        try {
            userDetailsService.loadUserByUsername(request.getUsername());
            throw new RuntimeException("用户名已存在");
        } catch (Exception e) {
            // 用户不存在，可以注册
        }
        
        // 创建用户（默认为VIEWER角色）
        User user = User.createEnabledUser(
                null,
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                Set.of(Role.VIEWER)
        );
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        
        // 保存用户（由具体服务实现）
        // 这里需要子模块实现保存逻辑
        
        // 生成Token
        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), 
                Set.of(Role.VIEWER.getAuthority()));
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .username(user.getUsername())
                .role(Role.VIEWER.name())
                .build();
    }
    
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // 验证刷新Token
        if (!jwtTokenProvider.validateToken(refreshToken) || 
            !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("无效的刷新Token");
        }
        
        // 获取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // 加载用户信息
        User user = userDetailsService.loadUserByUsername(username);
        
        // 生成新的访问Token
        String accessToken = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getAuthority)
                        .collect(java.util.stream.Collectors.toSet())
        );
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken) // 刷新Token保持不变
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .username(user.getUsername())
                .role(user.getRoles().iterator().next().name())
                .build();
    }
    
    @Override
    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    @Override
    public String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
    
    @Override
    public User getCurrentUser() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
}
