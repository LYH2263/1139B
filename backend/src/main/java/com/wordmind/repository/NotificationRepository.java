package com.wordmind.repository;

import com.wordmind.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndRead(Long userId, Boolean read, Pageable pageable);

    long countByUserIdAndRead(Long userId, Boolean read);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
