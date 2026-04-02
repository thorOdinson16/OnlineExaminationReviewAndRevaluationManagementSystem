package com.team.revaluation.service;

import com.team.revaluation.model.Evaluator;
import com.team.revaluation.model.Revaluator;
import com.team.revaluation.model.User;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserService — owns all User management business logic.
 *
 * AdminController was previously calling userRepository directly.
 * All of that logic now lives here, keeping the controller thin.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getUsersByRole(String role) {
        List<User> all = userRepository.findAll();
        if (role == null || role.isEmpty() || "ALL".equals(role)) {
            return all;
        }
        return all.stream()
                .filter(u -> role.equals(u.getRole()))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getUserStats() {
        List<User> all = userRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        stats.put("students",    count(all, "STUDENT"));
        stats.put("evaluators",  count(all, "EVALUATOR"));
        stats.put("revaluators", count(all, "REVALUATOR"));
        stats.put("admins",      count(all, "ADMIN"));
        return stats;
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    public List<Revaluator> getRevaluators() {
        return userRepository.findAll().stream()
                .filter(u -> "REVALUATOR".equals(u.getRole()))
                .map(u -> (Revaluator) u)
                .collect(Collectors.toList());
    }

    public List<Evaluator> getEvaluators() {
        return userRepository.findAll().stream()
                .filter(u -> "EVALUATOR".equals(u.getRole()))
                .map(u -> (Evaluator) u)
                .collect(Collectors.toList());
    }

    private long count(List<User> users, String role) {
        return users.stream().filter(u -> role.equals(u.getRole())).count();
    }
}