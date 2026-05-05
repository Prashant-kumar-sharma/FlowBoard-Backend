package com.flowboard.auth.repository;

import com.flowboard.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(User.Role role);

    long countByRoleAndIsActiveTrue(User.Role role);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByFullNameOrUsername(@Param("name") String name);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
}
