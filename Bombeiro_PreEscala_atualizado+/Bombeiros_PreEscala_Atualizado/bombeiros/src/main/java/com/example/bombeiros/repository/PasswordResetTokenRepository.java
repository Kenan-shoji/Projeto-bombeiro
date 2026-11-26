package com.example.bombeiros.repository;
import com.example.bombeiros.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByBombeiroId(Integer bombeiroId);
}
