package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "level_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "level_id"})
})
@Data
public class LevelProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "level_id", nullable = false)
    private Long levelId;
    
    @Column(name = "best_score")
    private Integer bestScore;
    
    @Column
    private Integer stars;
    
    @Column(nullable = false)
    private Boolean completed;
    
    @Column(nullable = false)
    private Integer attempts;
    
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
