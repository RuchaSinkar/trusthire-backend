package com.example.demo.service;

import com.example.demo.dto.AuthDto;
import com.example.demo.entity.User;
import com.example.demo.exception.BadRequestException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService  userDetailsService;
    private final JwtUtil             jwtUtil;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new BadRequestException("Username '" + req.getUsername() + "' is already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email '" + req.getEmail() + "' is already registered");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        var details = userDetailsService.loadUserByUsername(user.getUsername());
        return AuthDto.AuthResponse.builder()
                .token(jwtUtil.generateToken(details))
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        // Throws BadCredentialsException if wrong — caught by GlobalExceptionHandler
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        var details = userDetailsService.loadUserByUsername(req.getUsername());
        var user    = userRepository.findByUsername(req.getUsername()).orElseThrow();

        return AuthDto.AuthResponse.builder()
                .token(jwtUtil.generateToken(details))
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}