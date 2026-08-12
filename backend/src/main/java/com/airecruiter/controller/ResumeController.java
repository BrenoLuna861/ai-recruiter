package com.airecruiter.controller;

import com.airecruiter.dto.response.ResumeResponse;
import com.airecruiter.service.ResumeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
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

    /**
     * Aderência entre um currículo e uma vaga externa.
     *
     * A descrição vem no corpo porque a vaga é de outro portal e não está no
     * nosso banco. O currículo vem por id — o texto sai do banco.
     */
    @PostMapping("/{id}/match")
    public ResponseEntity<String> match(@PathVariable Long id,
                                        @Valid @RequestBody MatchRequest req,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        String json = resumeService.matchToJob(
            id, req.getJobTitle(), req.getJobDescription(), userDetails.getUsername());
        return ResponseEntity.ok(json);
    }

    @Data
    public static class MatchRequest {
        private String jobTitle;
        @NotBlank
        @Size(max = 12000)
        private String jobDescription;
    }

    /** Reescrita do currículo. O texto vem do banco, o cliente só informa o id. */
    @PostMapping("/{id}/improve")
    public ResponseEntity<Map<String, String>> improve(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        String melhorado = resumeService.improveResume(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("improvedResume", melhorado));
    }
}
