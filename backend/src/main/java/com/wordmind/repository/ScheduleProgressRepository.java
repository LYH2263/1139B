package com.wordmind.repository;

import com.wordmind.entity.ScheduleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleProgressRepository extends JpaRepository<ScheduleProgress, Long> {

    List<ScheduleProgress> findByScheduleIdOrderByDateAsc(Long scheduleId);

    Optional<ScheduleProgress> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

    List<ScheduleProgress> findByScheduleIdAndDateBetweenOrderByDateAsc(Long scheduleId, LocalDate startDate, LocalDate endDate);

    void deleteByScheduleId(Long scheduleId);
}
