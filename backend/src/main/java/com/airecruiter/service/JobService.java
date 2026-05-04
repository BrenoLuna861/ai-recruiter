package com.airecruiter.service;

import com.airecruiter.dto.request.JobRequest;
import com.airecruiter.entity.Application;
import com.airecruiter.entity.Job;
import com.airecruiter.entity.Resume;
import com.airecruiter.entity.User;
import com.airecruiter.repository.ApplicationRepository;
import com.airecruiter.repository.JobRepository;
import com.airecruiter.repository.ResumeRepository;
import com.airecruiter.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AnthropicService anthropicService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Job> getAllActive() {
        return jobRepository.findByActiveTrue();
    }

    public List<Job> getRecruiterJobs(String email) {
        User recruiter = getUser(email);
        return jobRepository.findByRecruiterIdAndActiveTrue(recruiter.getId());
    }

    @Transactional
    public Job createJob(JobRequest req, String email) {
        User recruiter = getUser(email);
        Job job = Job.builder()
            .recruiter(recruiter).title(req.getTitle()).company(req.getCompany())
            .description(req.getDescription()).requirements(req.getRequirements())
            .location(req.getLocation()).salaryRange(req.getSalaryRange())
            .jobType(req.getJobType()).active(true).build();
        return jobRepository.save(job);
    }

    @Transactional
    public Application applyToJob(Long jobId, Long resumeId, String email) {
        User candidate = getUser(email);
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, candidate.getId())
            .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidate.getId()))
            throw new IllegalArgumentException("Already applied to this job");

        Application app = Application.builder()
            .job(job).candidate(candidate).resume(resume).status(Application.Status.PENDING).build();

        // AI match scoring
        try {
            String matchJson = anthropicService.matchResumeToJob(resume.getContent(), job.getDescription());
            Map<?, ?> data = objectMapper.readValue(matchJson, Map.class);
            Object score = data.get("matchScore");
            if (score instanceof Number n) app.setMatchScore(n.intValue());
            app.setAiFeedback((String) data.get("analysis"));
        } catch (Exception e) {
            log.warn("Match scoring failed: {}", e.getMessage());
        }

        return applicationRepository.save(app);
    }

    public List<Application> getRanking(Long jobId, String email) {
        User recruiter = getUser(email);
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (!job.getRecruiter().getId().equals(recruiter.getId()))
            throw new SecurityException("Not your job");
        return applicationRepository.findByJobIdOrderByMatchScoreDesc(jobId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
