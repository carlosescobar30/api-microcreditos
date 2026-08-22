package com.carlosescobar30.apimicrocreditos.iam.repository;

import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername (String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);


}
