package com.wordmind.repository;

import com.wordmind.entity.MemoryAssociation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryAssociationRepository extends JpaRepository<MemoryAssociation, Long> {

    List<MemoryAssociation> findByWordId(Long wordId, Sort sort);

    boolean existsByWordIdAndTypeAndContentAndIsSystemGeneratedTrue(Long wordId, String type, String content);
}
