package com.versus.api.users.repo;

import com.versus.api.users.Role;
import com.versus.api.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    long countByIsActiveTrue();

    @Query("""
            SELECT u FROM User u
            WHERE u.status = com.versus.api.users.UserStatus.ACTIVE
              AND (:role IS NULL OR u.role = :role)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findAdminUsers(@Param("role") Role role,
                              @Param("search") String search,
                              Pageable pageable);
}
