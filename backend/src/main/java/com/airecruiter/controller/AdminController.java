package com.airecruiter.controller;

import com.airecruiter.entity.User;
import com.airecruiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    record UserDto(Long id, String name, String email, String role, boolean active, LocalDateTime createdAt) {}

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> users = userRepository.findAll().stream()
            .map(u -> new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isActive(), u.getCreatedAt()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            user.setRole(User.Role.valueOf(body.get("role")));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Role updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
        }
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(body.get("active"));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }
}