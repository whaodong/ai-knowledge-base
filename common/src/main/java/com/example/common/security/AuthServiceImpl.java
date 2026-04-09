package com.example.common.security;

import com.example.common.enums.ErrorCode;
import com.example.common.exception.BusinessException;
import com.example.common.security.dto.AuthResponse;
import com.example.common.security.dto.LoginRequest;
import com.example.common.security.dto.RefreshTokenRequest;
import com.example.common.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final ObjectProvider<UserPersistenceService> userPersistenceServiceProvider;
    
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
        UserPersistenceService userPersistenceService = userPersistenceServiceProvider.getIfAvailable();

        // 优先使用持久化层做唯一性判断，避免通过异常控制流程
        if (userPersistenceService != null && userPersistenceService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        // 回退到UserDetailsService实现（兼容无持久化层场景）
        if (userPersistenceService == null) {
            try {
                userDetailsService.loadUserByUsername(request.getUsername());
                throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
            } catch (UsernameNotFoundException e) {
                // 用户不存在，可以注册
            }
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
        
        // 保存用户（默认实现保存到内存；自定义实现可在各子模块接管）
        saveUser(user, userPersistenceService);
        
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
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的刷新Token");
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

    /**
     * 保存用户信息。
     *
     * @param user 用户对象
     * @param userPersistenceService 用户持久化服务
     */
    private void saveUser(User user, UserPersistenceService userPersistenceService) {
        if (userPersistenceService != null) {
            userPersistenceService.saveUser(user);
            return;
        }
        if (userDetailsService instanceof UserPersistenceService localPersistenceService) {
            localPersistenceService.saveUser(user);
        }
    }
}
