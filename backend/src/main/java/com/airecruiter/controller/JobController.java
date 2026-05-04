package com.airecruiter.controller;

import com.airecruiter.dto.request.JobRequest;
import com.airecruiter.entity.Application;
import com.airecruiter.entity.Job;
import com.airecruiter.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> list() {
        return ResponseEntity.ok(jobService.getAllActive());
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<Job>> myJobs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(jobService.getRecruiterJobs(userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> create(@Valid @RequestBody JobRequest req,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(201).body(jobService.createJob(req, userDetails.getUsername()));
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Map<String, String>> apply(
        @PathVariable Long id,
        @RequestParam Long resumeId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        jobService.applyToJob(id, resumeId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Candidatura enviada com sucesso!"));
    }

    @GetMapping("/{id}/ranking")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<Application>> ranking(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(jobService.getRanking(id, userDetails.getUsername()));
    }
}
