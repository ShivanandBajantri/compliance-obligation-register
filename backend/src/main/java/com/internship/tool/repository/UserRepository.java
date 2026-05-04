package com.internship.tool.repository;

import com.internship.tool.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Load the user together with its roles in a single LEFT JOIN query.
     *
     * Without @EntityGraph, Hibernate issues a second SELECT for the user_roles
     * join table every time Spring Security calls loadUserByUsername — a classic
     * N+1 pattern that fires on every authenticated request.
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);
}
