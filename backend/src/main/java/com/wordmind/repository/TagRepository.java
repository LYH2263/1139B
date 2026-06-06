package com.wordmind.repository;

import com.wordmind.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Tag> findByUserIdAndId(Long userId, Long id);

    Optional<Tag> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);

    @Query("SELECT COUNT(t) FROM Tag t WHERE t.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
