package com.wordmind.repository;

import com.wordmind.entity.LevelProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LevelProgressRepository extends JpaRepository<LevelProgress, Long> {
    
    Optional<LevelProgress> findByUserIdAndLevelId(Long userId, Long levelId);
    
    List<LevelProgress> findByUserId(Long userId);
    
    boolean existsByUserIdAndLevelId(Long userId, Long levelId);
    
    int countByUserIdAndCompletedTrue(Long userId);
}
