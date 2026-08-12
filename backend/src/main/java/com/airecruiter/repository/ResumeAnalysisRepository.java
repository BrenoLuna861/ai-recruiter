package com.airecruiter.repository;

import com.airecruiter.entity.ResumeAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeAnalysisRepository extends MongoRepository<ResumeAnalysis, String> {
    Optional<ResumeAnalysis> findByResumeId(Long resumeId);

    void deleteByUserId(Long userId);
}
