package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_associations")
@Data
public class MemoryAssociation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private Integer upvotes = 0;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "is_system_generated", nullable = false)
    private Boolean isSystemGenerated = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
