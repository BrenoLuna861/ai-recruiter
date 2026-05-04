package com.airecruiter.dto.response;

import com.airecruiter.entity.Resume;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ResumeResponse {
    private Long id;
    private String title;
    private String fileType;
    private Integer overallScore;
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer formatScore;
    private Integer atsScore;
    private Resume.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String analysis; // Full analysis text from MongoDB
}
