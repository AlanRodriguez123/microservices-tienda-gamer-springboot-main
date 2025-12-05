package com.tiendagamer.authservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tiendagamer.authservice.model.User;

public interface UserRepository extends JpaRepository<User,UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
}
