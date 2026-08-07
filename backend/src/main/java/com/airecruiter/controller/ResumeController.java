package com.airecruiter.controller;

import com.airecruiter.dto.response.ResumeResponse;
import com.airecruiter.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/analyze")
    public ResponseEntity<ResumeResponse> analyze(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "title", required = false) String title,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(resumeService.analyzeResume(file, title, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resumeService.getUserResumes(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resumeService.getResume(id, userDetails.getUsername()));
    }

    /** Reescrita do currículo. O texto vem do banco, o cliente só informa o id. */
    @PostMapping("/{id}/improve")
    public ResponseEntity<Map<String, String>> improve(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        String melhorado = resumeService.improveResume(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("improvedResume", melhorado));
    }
}
