package com.wordmind.repository;

import com.wordmind.entity.WordTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordTagRepository extends JpaRepository<WordTag, Long> {

    List<WordTag> findByUserIdAndWordId(Long userId, Long wordId);

    Optional<WordTag> findByUserIdAndWordIdAndTagId(Long userId, Long wordId, Long tagId);

    boolean existsByUserIdAndWordIdAndTagId(Long userId, Long wordId, Long tagId);

    void deleteByUserIdAndWordIdAndTagId(Long userId, Long wordId, Long tagId);

    void deleteByUserIdAndTagId(Long userId, Long tagId);

    @Query("SELECT wt.wordId FROM WordTag wt WHERE wt.userId = :userId AND wt.tagId = :tagId")
    List<Long> findWordIdsByUserIdAndTagId(@Param("userId") Long userId, @Param("tagId") Long tagId);

    @Query("SELECT wt.tagId FROM WordTag wt WHERE wt.userId = :userId AND wt.wordId = :wordId")
    List<Long> findTagIdsByUserIdAndWordId(@Param("userId") Long userId, @Param("wordId") Long wordId);

    @Query("SELECT wt.wordId FROM WordTag wt WHERE wt.userId = :userId AND wt.tagId IN :tagIds")
    List<Long> findWordIdsByUserIdAndTagIds(@Param("userId") Long userId, @Param("tagIds") List<Long> tagIds);
}
