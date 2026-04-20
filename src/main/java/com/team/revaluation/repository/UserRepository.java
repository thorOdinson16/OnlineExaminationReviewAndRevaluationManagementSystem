package com.team.revaluation.repository;

import com.team.revaluation.model.Revaluator;
import com.team.revaluation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * FIX: Fetch specifically as Revaluator subtype to avoid ClassCastException.
     * Using userRepository.findById() returns a User proxy that cannot be cast
     * to Revaluator even when the underlying row is one. This typed query
     * returns the concrete Revaluator entity directly.
     */
    @Query("SELECT r FROM Revaluator r WHERE r.userId = :id")
    Optional<Revaluator> findRevaluatorById(@Param("id") Long id);

    /**
     * Returns all users with REVALUATOR role as typed Revaluator entities.
     * Used by admin dropdown to populate assignable revaluators.
     */
    @Query("SELECT r FROM Revaluator r")
    List<Revaluator> findAllRevaluators();
}