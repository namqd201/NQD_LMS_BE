package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.QuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionTagRepository extends JpaRepository<QuestionTag, UUID> {
    Optional<QuestionTag> findByNameIgnoreCase(String name);
}
