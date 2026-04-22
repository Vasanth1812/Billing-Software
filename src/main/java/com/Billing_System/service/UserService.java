package com.Billing_System.service;

import com.Billing_System.dto.UserDTO;
import com.Billing_System.entity.User;
import com.Billing_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Get all active users */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /** Get user by ID */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    /** Create a new user (admin creates accounts, no self-registration) */
    public User createUser(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("User with email '" + dto.getEmail() + "' already exists");
        }
        if (userRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("User with user ID '" + dto.getUserId() + "' already exists");
        }

        User user = User.builder()
                .userId(dto.getUserId())
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))   // BCrypt hashed!
                .role(dto.getRole())
                .build();
        return userRepository.save(user);
    }

    /** Update a user's details */
    public User updateUser(UUID id, UserDTO dto) {
        User user = getUserById(id);

        // Allow email update only if not taken by another user
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already taken");
        }

        // Allow userId update only if not taken by another user
        if (!user.getUserId().equals(dto.getUserId()) && userRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("User ID '" + dto.getUserId() + "' is already taken");
        }

        user.setUserId(dto.getUserId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));  // BCrypt hashed!
        }
        return userRepository.save(user);
    }

    /** Soft delete a user */
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }
}
