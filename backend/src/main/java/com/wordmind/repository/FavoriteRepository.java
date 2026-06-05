package com.wordmind.repository;

import com.wordmind.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    Optional<Favorite> findByUserIdAndWordId(Long userId, Long wordId);
    
    boolean existsByUserIdAndWordId(Long userId, Long wordId);
    
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT f.wordId FROM Favorite f WHERE f.userId = :userId")
    List<Long> findFavoriteWordIdsByUserId(@Param("userId") Long userId);
    
    void deleteByUserIdAndWordId(Long userId, Long wordId);
}
