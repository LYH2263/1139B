package com.wordmind.repository;

import com.wordmind.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    
    @Query("SELECT rr FROM ReviewRecord rr WHERE rr.userId = :userId AND " +
           "(rr.nextReviewAt IS NULL OR rr.nextReviewAt <= :now) " +
           "ORDER BY rr.nextReviewAt ASC")
    List<ReviewRecord> findTodayReviews(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    Optional<ReviewRecord> findByUserIdAndWordId(Long userId, Long wordId);
    
    @Query("SELECT COUNT(rr) FROM ReviewRecord rr WHERE rr.userId = :userId AND rr.result = 'KNOWN'")
    Long countKnownWordsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(rr) FROM ReviewRecord rr WHERE rr.userId = :userId AND " +
           "rr.createdAt >= :startOfDay AND rr.createdAt < :endOfDay")
    Long countTodayReviewsByUserId(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(DISTINCT rr.wordId) FROM ReviewRecord rr WHERE rr.userId = :userId AND rr.proficiency >= 4")
    Long countMasteredWordsByUserId(@Param("userId") Long userId);

    @Query("SELECT rr.userId, COUNT(DISTINCT rr.wordId) FROM ReviewRecord rr " +
           "WHERE rr.proficiency >= 4 " +
           "GROUP BY rr.userId " +
           "ORDER BY COUNT(DISTINCT rr.wordId) DESC")
    List<Object[]> findMasteredWordsRanking();

    @Query(value = "SELECT DISTINCT CAST(rr.created_at AS DATE) FROM review_records rr WHERE rr.user_id = :userId ORDER BY 1 DESC", nativeQuery = true)
    List<Object> findDistinctReviewDatesByUserId(@Param("userId") Long userId);

    @Query("SELECT rr.userId FROM ReviewRecord rr GROUP BY rr.userId")
    List<Long> findAllUserIdsWithRecords();
}
