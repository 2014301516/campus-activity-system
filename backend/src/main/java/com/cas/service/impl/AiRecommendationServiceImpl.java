package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cas.config.DeepSeekProperties;
import com.cas.dto.AiRecommendationDTO;
import com.cas.entity.Activity;
import com.cas.entity.Category;
import com.cas.entity.Registration;
import com.cas.entity.Review;
import com.cas.entity.SignIn;
import com.cas.service.ActivityService;
import com.cas.service.AiRecommendationService;
import com.cas.service.CategoryService;
import com.cas.service.RegistrationService;
import com.cas.service.ReviewService;
import com.cas.service.SignInService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 推荐服务实现
 */
@Service
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private static final int MAX_CANDIDATE_COUNT = 8;
    private static final int MAX_RESULT_COUNT = 4;
    private static final Logger log = LoggerFactory.getLogger(AiRecommendationServiceImpl.class);

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private SignInService signInService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DeepSeekProperties deepSeekProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<AiRecommendationDTO> getRecommendations(Long userId) {
        activityService.refreshActivityStatuses();

        List<Registration> registrations = registrationService.lambdaQuery()
                .eq(Registration::getUserId, userId)
                .orderByDesc(Registration::getRegisteredAt)
                .list();
        List<Review> reviews = reviewService.lambdaQuery()
                .eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreatedAt)
                .list();
        List<SignIn> signIns = signInService.lambdaQuery()
                .eq(SignIn::getUserId, userId)
                .orderByDesc(SignIn::getSignInTime)
                .list();

        UserPreferenceProfile profile = buildUserProfile(registrations, reviews, signIns);
        Set<Long> excludedIds = registrations.stream()
                .filter(item -> "registered".equals(item.getStatus()))
                .map(Registration::getActivityId)
                .collect(Collectors.toSet());

        List<Activity> candidateActivities = loadCandidateActivities(excludedIds);
        if (candidateActivities.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, String> categoryNameMap = categoryService.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        candidateActivities.forEach(item -> item.setCategoryName(categoryNameMap.getOrDefault(item.getCategoryId(), "未分类")));

        List<ScoredActivity> rankedCandidates = candidateActivities.stream()
                .map(activity -> new ScoredActivity(activity, calculateScore(activity, profile)))
                .sorted(Comparator.comparingInt(ScoredActivity::getScore).reversed()
                        .thenComparing(item -> item.getActivity().getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_CANDIDATE_COUNT)
                .collect(Collectors.toList());

        Map<Long, RecommendationText> reasonMap = generateReasonsByDeepSeek(profile, rankedCandidates);

        return rankedCandidates.stream()
                .limit(MAX_RESULT_COUNT)
                .map(item -> toDto(item, reasonMap.get(item.getActivity().getId()), profile))
                .collect(Collectors.toList());
    }

    private UserPreferenceProfile buildUserProfile(List<Registration> registrations, List<Review> reviews, List<SignIn> signIns) {
        UserPreferenceProfile profile = new UserPreferenceProfile();
        Map<Integer, String> categoryNameMap = categoryService.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Set<Long> historyActivityIds = new HashSet<>();
        registrations.stream().map(Registration::getActivityId).forEach(historyActivityIds::add);
        reviews.stream().map(Review::getActivityId).forEach(historyActivityIds::add);
        signIns.stream().map(SignIn::getActivityId).forEach(historyActivityIds::add);

        Map<Long, Activity> historyActivityMap = loadActivitiesByIds(historyActivityIds);

        for (Registration registration : registrations) {
            Activity activity = historyActivityMap.get(registration.getActivityId());
            if (activity == null) {
                continue;
            }
            int weight = "registered".equals(registration.getStatus()) ? 14 : 6;
            increaseCategoryWeight(profile.categoryPreference, activity.getCategoryId(), weight);
            increaseTimeSlotWeight(profile.timeSlotPreference, timeSlotOf(activity.getStartTime()), 3);
        }

        for (SignIn signIn : signIns) {
            Activity activity = historyActivityMap.get(signIn.getActivityId());
            if (activity == null) {
                continue;
            }
            increaseCategoryWeight(profile.categoryPreference, activity.getCategoryId(), 18);
            increaseTimeSlotWeight(profile.timeSlotPreference, timeSlotOf(activity.getStartTime()), 5);
        }

        for (Review review : reviews) {
            Activity activity = historyActivityMap.get(review.getActivityId());
            if (activity == null) {
                continue;
            }
            int rating = review.getRating() == null ? 3 : review.getRating();
            increaseCategoryWeight(profile.categoryPreference, activity.getCategoryId(), rating * 6);
            increaseTimeSlotWeight(profile.timeSlotPreference, timeSlotOf(activity.getStartTime()), Math.max(rating, 1));
        }

        profile.historySummary = buildHistorySummary(profile, registrations, reviews, signIns, categoryNameMap);
        profile.favoriteCategoryIds = profile.categoryPreference.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        profile.favoriteCategoryNames = profile.favoriteCategoryIds.stream()
                .map(id -> categoryNameMap.getOrDefault(id, "未分类"))
                .collect(Collectors.toList());
        profile.favoriteTimeSlot = profile.timeSlotPreference.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("时间偏好暂不明显");
        return profile;
    }

    private Map<Long, Activity> loadActivitiesByIds(Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return new HashMap<>();
        }
        return activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, item -> item));
    }

    private List<Activity> loadCandidateActivities(Set<Long> excludedIds) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Activity::getStatus, "approved", "ongoing")
                .orderByDesc(Activity::getCurrentParticipants)
                .orderByAsc(Activity::getStartTime);
        if (excludedIds != null && !excludedIds.isEmpty()) {
            wrapper.notIn(Activity::getId, excludedIds);
        }
        return activityService.list(wrapper).stream()
                .filter(item -> item.getStartTime() == null || item.getEndTime() == null || item.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }

    private int calculateScore(Activity activity, UserPreferenceProfile profile) {
        int score = 35;

        if (profile.favoriteCategoryIds.contains(activity.getCategoryId())) {
            score += 28;
        } else {
            score += profile.categoryPreference.getOrDefault(activity.getCategoryId(), 0) / 2;
        }

        String slot = timeSlotOf(activity.getStartTime());
        score += profile.timeSlotPreference.getOrDefault(slot, 0) * 2;

        if ("approved".equals(activity.getStatus())) {
            score += 8;
        }
        if ("ongoing".equals(activity.getStatus())) {
            score += 4;
        }

        if (activity.getCurrentParticipants() != null) {
            score += Math.min(activity.getCurrentParticipants(), 18);
        }

        if (activity.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (activity.getStartTime().isAfter(now) && activity.getStartTime().isBefore(now.plusDays(7))) {
                score += 10;
            } else if (activity.getStartTime().isAfter(now.plusDays(30))) {
                score -= 4;
            }
        }

        if (!StringUtils.hasText(activity.getCoverImage())) {
            score -= 2;
        }

        return score;
    }

    private Map<Long, RecommendationText> generateReasonsByDeepSeek(UserPreferenceProfile profile, List<ScoredActivity> rankedCandidates) {
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            log.warn("DeepSeek API key 未配置，AI 推荐回退到 fallback 文案");
            return new HashMap<>();
        }

        try {
            String prompt = buildPrompt(profile, rankedCandidates);
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", deepSeekProperties.getModel());
            requestBody.put("stream", false);
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("thinking", Map.of("type", "disabled"));
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是校园活动推荐助手，正在为学生首页生成推荐文案。你的语气要自然、具体、像在给同学提建议，而不是写系统分析报告。请严格返回 JSON，不要输出额外解释。"),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekProperties.getApiKey());

            RestTemplate restTemplate = createRestTemplate();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String requestUrl = deepSeekProperties.getBaseUrl() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                return new HashMap<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String contentText = extractContentText(contentNode);
            if (!StringUtils.hasText(contentText)) {
                log.warn("DeepSeek 返回 content 为空，响应体片段: {}", truncateForLog(response.getBody()));
                return new HashMap<>();
            }

            log.info("DeepSeek 原始推荐内容: {}", truncateForLog(contentText));

            JsonNode jsonNode = parseRecommendationJson(contentText);
            JsonNode itemsNode = jsonNode.path("items");
            if (!itemsNode.isArray()) {
                log.warn("DeepSeek 返回内容未包含 items 数组: {}", truncateForLog(contentText));
                return new HashMap<>();
            }

            Map<Long, RecommendationText> result = new HashMap<>();
            for (JsonNode itemNode : itemsNode) {
                long activityId = itemNode.path("activityId").asLong(-1);
                if (activityId <= 0) {
                    continue;
                }
                RecommendationText text = new RecommendationText();
                text.reason = itemNode.path("reason").asText("");
                text.analysis = itemNode.path("analysis").asText("");
                text.tag = itemNode.path("tag").asText("");
                JsonNode highlightsNode = itemNode.path("highlights");
                if (highlightsNode.isArray()) {
                    text.highlights = new ArrayList<>();
                    for (JsonNode highlightNode : highlightsNode) {
                        if (StringUtils.hasText(highlightNode.asText())) {
                            text.highlights.add(highlightNode.asText());
                        }
                    }
                }
                result.put(activityId, text);
            }
            log.info("DeepSeek 推荐解析成功，命中 {} 条文案", result.size());
            return result;
        } catch (Exception e) {
            log.warn("DeepSeek 推荐解析失败，回退到 fallback 文案: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = deepSeekProperties.getTimeout() == null ? 30000 : deepSeekProperties.getTimeout();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private String extractContentText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    builder.append(item.asText()).append('\n');
                    continue;
                }
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText()).append('\n');
                    continue;
                }
                JsonNode contentTextNode = item.path("content");
                if (contentTextNode.isTextual()) {
                    builder.append(contentTextNode.asText()).append('\n');
                }
            }
            return builder.toString().trim();
        }
        return contentNode.toString();
    }

    private JsonNode parseRecommendationJson(String rawContent) throws Exception {
        List<String> candidates = new ArrayList<>();
        candidates.add(rawContent);

        String cleaned = rawContent.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
            candidates.add(cleaned.trim());
        }

        String extracted = extractFirstJsonObject(cleaned);
        if (StringUtils.hasText(extracted)) {
            candidates.add(extracted);
        }

        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            try {
                return objectMapper.readTree(candidate);
            } catch (Exception ignored) {
                // 尝试下一种兼容形式
            }
        }

        throw new IllegalArgumentException("无法从大模型返回内容中解析 JSON");
    }

    private String extractFirstJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        int start = text.indexOf('{');
        if (start < 0) {
            return "";
        }

        int braceDepth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                braceDepth++;
                continue;
            }
            if (ch == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return "";
    }

    private String truncateForLog(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 1200 ? normalized.substring(0, 1200) + "...(truncated)" : normalized;
    }

    private String buildPrompt(UserPreferenceProfile profile, List<ScoredActivity> rankedCandidates) throws Exception {
        Map<String, Object> promptObject = new LinkedHashMap<>();
        promptObject.put("task", "请为首页 AI 推荐卡片生成更像真实推荐助手的文案。读起来要像在对学生说话，而不是在输出系统规则说明。");
        promptObject.put("output_format",
                "返回 JSON 对象，格式为 {\"items\":[{\"activityId\":1,\"reason\":\"...\",\"analysis\":\"...\",\"tag\":\"...\",\"highlights\":[\"...\",\"...\"]}]}，tag 只允许使用：兴趣匹配、时间合适、热度较高、AI精选、值得尝试。");
        promptObject.put("user_profile", profile.historySummary);

        List<Map<String, Object>> candidates = rankedCandidates.stream().map(item -> {
            Activity activity = item.getActivity();
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("activityId", activity.getId());
            candidate.put("title", activity.getTitle());
            candidate.put("category", activity.getCategoryName());
            candidate.put("startTime", activity.getStartTime());
            candidate.put("timeSlot", timeSlotOf(activity.getStartTime()));
            candidate.put("location", activity.getLocation());
            candidate.put("currentParticipants", activity.getCurrentParticipants());
            candidate.put("maxParticipants", activity.getMaxParticipants());
            candidate.put("status", activity.getStatus());
            candidate.put("score", item.getScore());
            return candidate;
        }).collect(Collectors.toList());
        promptObject.put("candidates", candidates);
        promptObject.put("requirements", List.of(
                "reason 控制在30到50字之间，要像首页推荐语，简短自然，像在对学生说话，可以使用“如果你最近想”“这场会比较适合你”“可以优先看看”这类表达",
                "analysis 控制在38到68字之间，要像解释推荐原因，必须结合至少两个具体信息：兴趣方向、时间段、地点、报名热度中的任意两项或以上",
                "不要使用“根据分析”“较为”“虽非”“属性接近”“匹配度较高”“综合来看”等生硬书面化表达",
                "不同活动的 reason 和 analysis 开头尽量不同，避免四条文案像同一模板改词",
                "优先写出具体感受，例如“下午去信息楼参加讲座比较顺手”“晚上在西操场活动氛围更强”这类表达",
                "可以提活动标题、地点、时间、热度，但不要机械罗列参数，不要把句子写成报表",
                "highlights 返回 2 到 3 条短语，每条控制在4到10字之间，风格口语化，例如“讲座方向对口”“晚上更好安排”“报名热度不错”",
                "不要杜撰不存在的个人信息，不要使用绝对化措辞，不要夸张承诺"
        ));
        promptObject.put("style_examples", List.of(
                Map.of(
                        "good_reason", "如果你最近还想参加讲座，这场数据分析沙龙会比较对你的胃口，可以优先看看。",
                        "good_analysis", "你最近参加过学术讲座，这场又安排在信息楼的下午时段，而且已经有不少同学报名，去参与会比较顺手。"
                ),
                Map.of(
                        "good_reason", "如果你想找一场氛围更轻松的活动，这场夜跑会是个不错的选择。",
                        "good_analysis", "它安排在晚上西操场，参与门槛不高，现场人也会比较多，适合作为最近放松一下的活动。"
                )
        ));

        return objectMapper.writeValueAsString(promptObject);
    }

    private AiRecommendationDTO toDto(ScoredActivity item, RecommendationText llmText, UserPreferenceProfile profile) {
        Activity activity = item.getActivity();
        AiRecommendationDTO dto = new AiRecommendationDTO();
        dto.setId(activity.getId());
        dto.setTitle(activity.getTitle());
        dto.setDescription(activity.getDescription());
        dto.setCoverImage(activity.getCoverImage());
        dto.setCategoryName(activity.getCategoryName());
        dto.setLocation(activity.getLocation());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setStatus(activity.getStatus());
        dto.setCurrentParticipants(activity.getCurrentParticipants());
        dto.setMaxParticipants(activity.getMaxParticipants());
        dto.setScore(item.getScore());

        if (llmText != null && StringUtils.hasText(llmText.reason)) {
            dto.setReason(llmText.reason);
            dto.setAnalysis(StringUtils.hasText(llmText.analysis)
                    ? llmText.analysis
                    : buildFallbackAnalysis(activity, profile, item.getScore()));
            dto.setSource("deepseek");
            dto.setTag(StringUtils.hasText(llmText.tag) ? llmText.tag : fallbackTag(activity, profile));
            dto.setHighlights(llmText.highlights != null && !llmText.highlights.isEmpty()
                    ? llmText.highlights
                    : buildFallbackHighlights(activity, profile, item.getScore()));
        } else {
            dto.setReason(fallbackReason(activity, profile, item.getScore()));
            dto.setAnalysis(buildFallbackAnalysis(activity, profile, item.getScore()));
            dto.setSource("fallback");
            dto.setTag(fallbackTag(activity, profile));
            dto.setHighlights(buildFallbackHighlights(activity, profile, item.getScore()));
        }
        return dto;
    }

    private String fallbackReason(Activity activity, UserPreferenceProfile profile, int score) {
        String categoryName = StringUtils.hasText(activity.getCategoryName()) ? activity.getCategoryName() : "该分类";
        String timeSlot = timeSlotOf(activity.getStartTime());
        int participants = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        String title = StringUtils.hasText(activity.getTitle()) ? activity.getTitle() : "这场活动";

        if (profile.favoriteCategoryIds.contains(activity.getCategoryId()) && participants >= 10) {
            return title + "和你最近偏好的" + categoryName + "方向很贴近，而且已经有不少同学关注，值得你优先看看。";
        }
        if (activity.getStartTime() != null && activity.getStartTime().isBefore(LocalDateTime.now().plusDays(7))) {
            return title + "安排在近期的" + timeSlot + "，时间上更容易和你最近的参与节奏对上，临近参加也更方便。";
        }
        if (participants >= 10) {
            return title + "目前关注度已经起来了，如果你想先挑一个热度高、参与感更强的活动，它会是不错的选择。";
        }
        if (score >= 60) {
            return title + "和你之前报名、签到过的内容方向比较接近，看起来会更符合你近期的兴趣路线。";
        }
        return title + "和你之前参加的类型不完全一样，但也正因为这样，反而可能带来一次更有新鲜感的尝试。";
    }

    private String buildFallbackAnalysis(Activity activity, UserPreferenceProfile profile, int score) {
        String categoryName = StringUtils.hasText(activity.getCategoryName()) ? activity.getCategoryName() : "该分类";
        String location = StringUtils.hasText(activity.getLocation()) ? activity.getLocation() : "校内";
        String timeSlot = timeSlotOf(activity.getStartTime());
        int participants = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        int maxParticipants = activity.getMaxParticipants() == null || activity.getMaxParticipants() <= 0 ? 0 : activity.getMaxParticipants();
        String favoriteCategories = profile.favoriteCategoryNames.isEmpty()
                ? "你最近的兴趣方向"
                : String.join("、", profile.favoriteCategoryNames);

        if (profile.favoriteCategoryIds.contains(activity.getCategoryId()) && participants >= 10) {
            return "你最近更常关注" + favoriteCategories + "这类活动，而这场活动正好属于" + categoryName + "方向，地点在"
                    + location + "，同时已有" + participants + "位同学参与，匹配度和热度都比较在线。";
        }

        if (activity.getStartTime() != null && timeSlot.equals(profile.favoriteTimeSlot)) {
            return "你最近更常在" + profile.favoriteTimeSlot + "参加活动，这场安排在" + timeSlot + "，地点是" + location
                    + "，时间上比较贴合你的参与习惯，临时安排行程也更顺。";
        }

        if (maxParticipants > 0 && participants > 0) {
            int ratio = participants * 100 / maxParticipants;
            return "这场活动目前已有" + participants + "人报名，约达到名额的" + ratio + "%，整体关注度不错；再结合你近期对"
                    + favoriteCategories + "方向的偏好，所以把它排在了前面。";
        }

        if (score >= 60) {
            return "系统综合你最近的报名、签到和评价偏好后，发现这场活动和你常关注的" + favoriteCategories
                    + "方向有一定重合，而且时间和节奏也比较容易衔接。";
        }

        return "这场活动虽然和你以往参加的内容不完全相同，但类型上有一定延展性，适合在保持原有兴趣之外，再尝试一个新的方向。";
    }

    private String fallbackTag(Activity activity, UserPreferenceProfile profile) {
        if (profile.favoriteCategoryIds.contains(activity.getCategoryId())) {
            return "兴趣匹配";
        }
        if (activity.getStartTime() != null && activity.getStartTime().isBefore(LocalDateTime.now().plusDays(7))) {
            return "时间合适";
        }
        if (activity.getCurrentParticipants() != null && activity.getCurrentParticipants() >= 10) {
            return "热度较高";
        }
        if (profile.historySummary.contains("偏好分类：暂不明显")) {
            return "AI精选";
        }
        return "值得尝试";
    }

    private List<String> buildFallbackHighlights(Activity activity, UserPreferenceProfile profile, int score) {
        List<String> highlights = new ArrayList<>();

        if (profile.favoriteCategoryIds.contains(activity.getCategoryId())) {
            highlights.add("偏好分类接近");
        }
        if (activity.getStartTime() != null && timeSlotOf(activity.getStartTime()).equals(profile.favoriteTimeSlot)) {
            highlights.add("时间段更匹配");
        }
        if (activity.getCurrentParticipants() != null && activity.getCurrentParticipants() >= 10) {
            highlights.add("当前热度较高");
        }
        if (activity.getStartTime() != null && activity.getStartTime().isBefore(LocalDateTime.now().plusDays(7))) {
            highlights.add("近期即可参加");
        }
        if (score >= 70) {
            highlights.add("综合匹配度高");
        }

        if (highlights.isEmpty()) {
            highlights.add("适合拓展兴趣");
            highlights.add("活动信息完整");
        }

        return highlights.stream().distinct().limit(3).collect(Collectors.toList());
    }

    private String buildHistorySummary(UserPreferenceProfile profile,
                                       List<Registration> registrations,
                                       List<Review> reviews,
                                       List<SignIn> signIns,
                                       Map<Integer, String> categoryNameMap) {
        List<String> favoriteCategories = profile.categoryPreference.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> categoryNameMap.getOrDefault(entry.getKey(), "未分类"))
                .collect(Collectors.toList());

        return "历史报名 " + registrations.size() + " 次，签到 " + signIns.size() + " 次，评价 " + reviews.size()
                + " 次；偏好分类：" + (favoriteCategories.isEmpty() ? "暂不明显" : String.join("、", favoriteCategories))
                + "；偏好时间：" + profile.favoriteTimeSlot + "。";
    }

    private void increaseCategoryWeight(Map<Integer, Integer> map, Integer categoryId, int weight) {
        if (categoryId == null) {
            return;
        }
        map.put(categoryId, map.getOrDefault(categoryId, 0) + weight);
    }

    private void increaseTimeSlotWeight(Map<String, Integer> map, String slot, int weight) {
        map.put(slot, map.getOrDefault(slot, 0) + weight);
    }

    private String timeSlotOf(LocalDateTime time) {
        if (time == null) {
            return "时间待定";
        }
        int hour = time.getHour();
        if (hour < 12) {
            return "上午";
        }
        if (hour < 18) {
            return "下午";
        }
        return "晚上";
    }

    private static class UserPreferenceProfile {
        private final Map<Integer, Integer> categoryPreference = new HashMap<>();
        private final Map<String, Integer> timeSlotPreference = new HashMap<>();
        private List<Integer> favoriteCategoryIds = new ArrayList<>();
        private List<String> favoriteCategoryNames = new ArrayList<>();
        private String favoriteTimeSlot = "时间偏好暂不明显";
        private String historySummary = "";
    }

    private static class ScoredActivity {
        private final Activity activity;
        private final Integer score;

        private ScoredActivity(Activity activity, Integer score) {
            this.activity = activity;
            this.score = score;
        }

        public Activity getActivity() {
            return activity;
        }

        public Integer getScore() {
            return score;
        }
    }

    private static class RecommendationText {
        private String reason;
        private String analysis;
        private String tag;
        private List<String> highlights;
    }
}
