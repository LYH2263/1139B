package com.wordmind.repository;

import com.wordmind.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {
    
    List<Level> findAllByOrderByOrderAsc();
    
    Optional<Level> findByOrder(Integer order);
    
    boolean existsByOrder(Integer order);
}
