package com.weathercody.api.repository;

import com.weathercody.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 이메일 중복 확인을 위한 메서드
    boolean existsByEmail(String email);

    // 이메일로 사용자 정보를 찾기 위한 메서드
    Optional<User> findByEmail(String email);
}