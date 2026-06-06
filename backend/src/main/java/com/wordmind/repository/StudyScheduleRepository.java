package com.wordmind.repository;

import com.wordmind.entity.StudySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyScheduleRepository extends JpaRepository<StudySchedule, Long> {

    List<StudySchedule> findByUserId(Long userId);

    List<StudySchedule> findByUserIdAndStatus(Long userId, StudySchedule.ScheduleStatus status);

    Optional<StudySchedule> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
