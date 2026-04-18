package com.team.revaluation.controller;

import com.team.revaluation.factory.UserFactory;
import com.team.revaluation.model.User;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.model.Student;
import com.team.revaluation.model.Evaluator;
import com.team.revaluation.model.Revaluator;
import com.team.revaluation.model.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserFactory userFactory;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");
        String role = request.get("role");

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        // Create user using factory method
        User user = userFactory.createUser(role, name, email, password);
        
        if (user instanceof Student s) {
            s.setUsn(request.get("usn"));
            s.setSection(request.get("section"));
        } else if (user instanceof Evaluator e) {
            e.setDepartment(request.get("department"));
        } else if (user instanceof Revaluator r) {
            r.setSpecialization(request.get("specialization"));
        } else if (user instanceof Admin a) {
            a.setAdminCode(request.get("adminCode"));
        }
        
        // Save user to database
        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", savedUser.getUserId());
        response.put("name", savedUser.getName());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        response.put("message", "User registered successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        
        User user = userOptional.get();
        
        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("message", "Login successful");
        
        return ResponseEntity.ok(response);
    }
}