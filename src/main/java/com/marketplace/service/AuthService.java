package com.marketplace.service;

import com.marketplace.dto.request.LoginRequest;
import com.marketplace.dto.request.RegisterRequest;
import com.marketplace.dto.response.AuthResponse;
import com.marketplace.dto.response.UserResponse;
import com.marketplace.entity.User;
import com.marketplace.enums.UserRole;
import com.marketplace.enums.UserStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.DuplicateResourceException;
import com.marketplace.repository.UserRepository;
import com.marketplace.security.JwtService;
import com.marketplace.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (request.role() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot self-register as ADMIN");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        return AuthResponse.of(token, UserResponse.fromEntity(user));
    }
}