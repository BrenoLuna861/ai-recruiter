package com.airecruiter.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resume_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalysis {

    @Id
    private String id;

    @Field("resume_id")
    private Long resumeId;

    @Field("user_id")
    private Long userId;

    @Field("full_analysis")
    private String fullAnalysis;

    @Field("summary")
    private String summary;

    @Field("strengths")
    private List<String> strengths;

    @Field("weaknesses")
    private List<String> weaknesses;

    @Field("suggestions")
    private List<String> suggestions;

    @Field("keywords_found")
    private List<String> keywordsFound;

    @Field("keywords_missing")
    private List<String> keywordsMissing;

    @Field("rewritten_summary")
    private String rewrittenSummary;

    @Field("overall_score")
    private Integer overallScore;

    @Field("skills_score")
    private Integer skillsScore;

    @Field("experience_score")
    private Integer experienceScore;

    @Field("format_score")
    private Integer formatScore;

    @Field("ats_score")
    private Integer atsScore;

    @Field("created_at")
    private LocalDateTime createdAt;
}
