package com.careidentityservice.service;


import com.carecommon.exception.ResourceNotFoundException;
import com.careidentityservice.dto.UserResponse;
import com.careidentityservice.mapper.UserMapper;
import com.careidentityservice.model.User;
import com.careidentityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserResponse response = userMapper.toUserResponse(user);
        response.setTwoFactorEnabled(twoFactorAuthService.isTwoFactorEnabled(user));
        
        return response;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserResponse response = userMapper.toUserResponse(user);
        response.setTwoFactorEnabled(twoFactorAuthService.isTwoFactorEnabled(user));
        
        return response;
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: {}", user.getEmail());
    }

    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User activated: {}", user.getEmail());
    }
}