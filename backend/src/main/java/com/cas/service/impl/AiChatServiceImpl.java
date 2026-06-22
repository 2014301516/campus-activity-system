package com.cas.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cas.config.DeepSeekProperties;
import com.cas.entity.Activity;
import com.cas.entity.Registration;
import com.cas.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired private DeepSeekProperties deepSeek;
    @Autowired private ActivityService activityService;
    @Autowired private RegistrationService registrationService;
    @Autowired private ReviewService reviewService;
    @Autowired private SignInService signInService;
    @Autowired private CategoryService categoryService;
    @Autowired private UserService userService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    @Override
    public Map<String, Object> ask(Long userId, String question, Long activityId) {
        // 无 API Key 降级
        if (!StringUtils.hasText(deepSeek.getApiKey())) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", "AI 问答功能尚未配置 DeepSeek API Key，请参考文档设置后重试。你可以先浏览首页的推荐活动，或点击活动卡片查看详情。");
            fallback.put("source", "fallback");
            return fallback;
        }

        // 构建上下文
        String context = buildContext(userId, activityId);
        String prompt = "你是校园活动助手。根据以下信息回答学生的问题。语气友好、具体，像同学间聊天。\n\n"
                + context + "\n\n学生问题：" + question;

        try {
            String answer = callDeepSeek(prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("source", "deepseek");
            return result;
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", "抱歉，AI 服务暂时不可用（" + e.getMessage() + "）。你可以浏览下方推荐活动或查看活动详情了解信息。");
            fallback.put("source", "fallback");
            return fallback;
        }
    }

    private String buildContext(Long userId, Long activityId) {
        StringBuilder sb = new StringBuilder();

        // 用户画像
        String userName = userService.getById(userId).getRealName();
        long regCount = registrationService.lambdaQuery().eq(Registration::getUserId, userId).eq(Registration::getStatus, "registered").count();
        long signCount = signInService.lambdaQuery().eq(com.cas.entity.SignIn::getUserId, userId).count();
        long reviewCount = reviewService.lambdaQuery().eq(com.cas.entity.Review::getUserId, userId).count();
        sb.append("【学生信息】姓名：").append(userName)
          .append("，历史报名 ").append(regCount)
          .append(" 次，签到 ").append(signCount)
          .append(" 次，评价 ").append(reviewCount).append(" 次。\n\n");

        // 当前活动上下文
        if (activityId != null) {
            Activity a = activityService.getById(activityId);
            if (a != null) {
                String catName = categoryService.getById(a.getCategoryId()).getName();
                sb.append("【当前查看的活动】").append(a.getTitle())
                  .append("（分类：").append(catName)
                  .append("，地点：").append(a.getLocation())
                  .append("，时间：").append(a.getStartTime().format(DTF))
                  .append(" ~ ").append(a.getEndTime().format(DTF))
                  .append("，已报名 ").append(a.getCurrentParticipants())
                  .append("/").append(a.getMaxParticipants()).append("）\n\n");
            }
        }

        // 可报名活动摘要
        List<Activity> available = activityService.lambdaQuery()
                .in(Activity::getStatus, "approved", "ongoing")
                .orderByAsc(Activity::getStartTime)
                .list().stream().limit(20).collect(Collectors.toList());

        sb.append("【当前可报名活动列表】\n");
        for (Activity a : available) {
            String catName = categoryService.getById(a.getCategoryId()).getName();
            sb.append("· ").append(a.getTitle())
              .append(" [").append(catName).append("] ")
              .append(a.getStartTime().format(DTF)).append(" ")
              .append(a.getLocation())
              .append("（").append(a.getCurrentParticipants()).append("/").append(a.getMaxParticipants()).append("）\n");
        }

        return sb.toString();
    }

    private String callDeepSeek(String prompt) {
        String url = deepSeek.getBaseUrl() + "/chat/completions";
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeek.getApiKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", deepSeek.getModel());
        body.put("stream", false);
        body.put("messages", Arrays.asList(
            Map.of("role", "system", "content", "你是校园活动助手，回答要简短（200字以内），语气亲切像同学聊天。可以使用Markdown格式让回答更清晰。"),
            Map.of("role", "user", "content", prompt)
        ));

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        JSONObject json = JSONUtil.parseObj(response.getBody());
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            return choices.getJSONObject(0).getJSONObject("message").getStr("content").trim();
        }
        throw new RuntimeException("DeepSeek 返回为空");
    }
}
