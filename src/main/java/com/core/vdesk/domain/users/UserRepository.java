package com.core.vdesk.domain.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface UserRepository extends JpaRepository<Users,Long> {

    Optional<Users> findByEmail(String email);
    boolean existsByEmail(@NotBlank @Size(max=120) @Email String email);
}
