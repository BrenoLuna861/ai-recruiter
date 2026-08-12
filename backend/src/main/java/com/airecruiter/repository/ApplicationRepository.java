package com.airecruiter.repository;

import com.airecruiter.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobIdOrderByMatchScoreDesc(Long jobId);
    List<Application> findByCandidateId(Long candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    void deleteByCandidateId(Long candidateId);

    /** Candidaturas recebidas nas vagas de um recrutador. */
    void deleteByJobIdIn(java.util.List<Long> jobIds);
}
