package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
