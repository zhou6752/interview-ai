package com.interviewai.controller;

import com.interviewai.common.exception.BusinessException;
import com.interviewai.common.exception.ErrorCode;
import com.interviewai.dto.LoginRequest;
import com.interviewai.dto.RegisterRequest;
import com.interviewai.entity.User;
import com.interviewai.repository.UserRepository;
import com.interviewai.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "注册和登录")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "注册", description = "创建新用户，密码使用 BCrypt 加密存储")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码不能少于 6 位");
        }
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        userRepo.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "注册成功");
        return result;
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "验证用户名密码，返回 JWT Token")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        return result;
    }
}
