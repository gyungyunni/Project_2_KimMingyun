package com.example.mutsasns.repository;

import com.example.mutsasns.entity.ArticleEntity;
import com.example.mutsasns.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    Page<ArticleEntity> findAllByUser(UserEntity user, Pageable pageable);
}
