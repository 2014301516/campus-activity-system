package com.cas.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 推荐结果
 */
@Data
public class AiRecommendationDTO {

    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String categoryName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private Integer score;
    private String reason;
    private String analysis;
    private String tag;
    private List<String> highlights;
}
