package com.acme.hrassistant.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findFirstByUsernameAndConsumedFalseOrderByCreatedAtDesc(String username);

    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetOtp o set o.consumed = true where o.username = :username and o.consumed = false")
    int consumeAllActiveForUsername(@Param("username") String username);
}
