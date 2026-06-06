package com.wordmind.repository;

import com.wordmind.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @Query("SELECT h FROM SearchHistory h WHERE h.userId = :userId ORDER BY h.searchedAt DESC")
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(@Param("userId") Long userId);

    @Query("SELECT h.keyword FROM SearchHistory h WHERE h.userId = :userId ORDER BY h.searchedAt DESC")
    List<String> findKeywordsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SearchHistory h WHERE h.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SearchHistory h WHERE h.id = :id AND h.userId = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SearchHistory h WHERE h.userId = :userId AND h.keyword = :keyword")
    void deleteByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT h.keyword, COUNT(h.keyword) as cnt FROM SearchHistory h GROUP BY h.keyword ORDER BY cnt DESC")
    List<Object[]> findHotKeywords();
}
