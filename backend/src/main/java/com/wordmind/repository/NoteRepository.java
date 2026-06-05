package com.wordmind.repository;

import com.wordmind.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    Optional<Note> findByUserIdAndWordId(Long userId, Long wordId);
    
    boolean existsByUserIdAndWordId(Long userId, Long wordId);
    
    Page<Note> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.content LIKE %:keyword%")
    Page<Note> findByUserIdAndContentContaining(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
    
    void deleteByUserIdAndWordId(Long userId, Long wordId);
}
