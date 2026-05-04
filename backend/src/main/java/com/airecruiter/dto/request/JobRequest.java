package com.airecruiter.dto.request;

import com.airecruiter.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobRequest {
    @NotBlank @Size(max = 255)
    private String title;

    @NotBlank @Size(max = 191)
    private String company;

    @NotBlank
    private String description;

    private String requirements;
    private String location;
    private String salaryRange;
    private Job.JobType jobType = Job.JobType.FULL_TIME;
}
