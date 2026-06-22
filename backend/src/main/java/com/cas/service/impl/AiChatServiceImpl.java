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
    public Map<String, Object> ask(Long userId, String question, Long activityId, List<Map<String, String>> messages, String page) {
        if (!StringUtils.hasText(deepSeek.getApiKey())) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", "AI 问答功能尚未配置 DeepSeek API Key，请参考文档设置后重试。");
            fallback.put("source", "fallback");
            return fallback;
        }

        List<Map<String, String>> msgs = new ArrayList<>();

        // 系统提示 + 上下文
        String context = buildContext(userId, activityId, page);
        msgs.add(Map.of("role", "system", "content", "你是校园活动助手，回答简短（200字内），可使用Markdown。\n\n" + context));

        // 历史消息
        if (messages != null) {
            for (Map<String, String> m : messages) {
                String role = "ai".equals(m.get("role")) ? "assistant" : "user";
                msgs.add(Map.of("role", role, "content", m.get("content")));
            }
        }

        // 当前问题
        msgs.add(Map.of("role", "user", "content", question));

        try {
            String answer = callDeepSeek(msgs);
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("source", "deepseek");
            return result;
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", "抱歉，AI 服务暂时不可用（" + e.getMessage() + "）。");
            fallback.put("source", "fallback");
            return fallback;
        }
    }

    private String buildContext(Long userId, Long activityId, String page) {
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

        // 页面上下文
        if (page != null) {
            sb.append("【当前页面】").append(page).append("\n");
            if ("manage".equals(page)) {
                List<Activity> myActs = activityService.lambdaQuery().eq(Activity::getOrganizerId, userId).list();
                sb.append("你发布的活动共").append(myActs.size()).append("个：\n");
                for (Activity a : myActs) {
                    sb.append("· ").append(a.getTitle()).append(" [").append(a.getStatus()).append("] 报名").append(a.getCurrentParticipants()).append("/").append(a.getMaxParticipants()).append("\n");
                }
            } else if ("admin".equals(page)) {
                long total = activityService.count();
                long pending = activityService.lambdaQuery().eq(Activity::getStatus, "pending").count();
                long users = userService.count();
                sb.append("系统概况：活动总数").append(total).append("，待审核").append(pending).append("，用户总数").append(users).append("\n");
            } else if ("my-activities".equals(page)) {
                long myReg = registrationService.lambdaQuery().eq(Registration::getUserId, userId).eq(Registration::getStatus, "registered").count();
                sb.append("你已报名").append(myReg).append("个活动\n");
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

    private String callDeepSeek(List<Map<String, String>> messages) {
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
        body.put("messages", messages);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        JSONObject json = JSONUtil.parseObj(response.getBody());
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            return choices.getJSONObject(0).getJSONObject("message").getStr("content").trim();
        }
        throw new RuntimeException("DeepSeek 返回为空");
    }

    @Override
    public String generateDescription(String title, Long categoryId) {
        if (!StringUtils.hasText(deepSeek.getApiKey())) {
            return "AI 功能尚未配置 API Key，请手动填写活动描述。";
        }
        String catName = categoryService.getById(categoryId).getName();
        String prompt = "为校园活动「" + title + "」写一段活动描述（分类：" + catName
                + "）。100-200字，吸引学生参加，不要用Markdown格式。";

        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", "你是校园活动文案助手，写吸引人的活动描述。"));
            msgs.add(Map.of("role", "user", "content", prompt));
            return callDeepSeek(msgs);
        } catch (Exception e) {
            return "AI 生成失败：" + e.getMessage();
        }
    }

    @Override
    public String getAuditSuggestion(Long activityId) {
        if (!StringUtils.hasText(deepSeek.getApiKey())) {
            return "AI 功能尚未配置 API Key。";
        }
        Activity activity = activityService.getById(activityId);
        if (activity == null) return "活动不存在";

        String catName = categoryService.getById(activity.getCategoryId()).getName();
        String systemPrompt;
        String prompt;

        if ("cancel_pending".equals(activity.getStatus())) {
            systemPrompt = "你是校园活动审核助手，评估取消申请是否合理。";
            prompt = "组织者申请取消以下活动，请评估是否应该同意取消（50-100字）：\n"
                    + "标题：" + activity.getTitle() + "\n"
                    + "分类：" + catName + "\n"
                    + "地点：" + activity.getLocation() + "\n"
                    + "时间：" + activity.getStartTime() + " ~ " + activity.getEndTime() + "\n"
                    + "已报名人数：" + activity.getCurrentParticipants() + "/" + activity.getMaxParticipants() + "\n"
                    + "取消理由：" + (activity.getCancelRequestReason() != null ? activity.getCancelRequestReason() : "未提供") + "\n"
                    + "请评估：1）取消理由是否充分 2）是否影响已报名学生 3）建议同意还是驳回";
        } else {
            systemPrompt = "你是校园活动审核助手，给出简洁专业的审核建议。";
            prompt = "请审核以下校园活动并给出建议（50-100字）：\n"
                    + "标题：" + activity.getTitle() + "\n"
                    + "分类：" + catName + "\n"
                    + "描述：" + activity.getDescription() + "\n"
                    + "地点：" + activity.getLocation() + "\n"
                    + "时间：" + activity.getStartTime() + " ~ " + activity.getEndTime() + "\n"
                    + "人数上限：" + activity.getMaxParticipants() + "\n"
                    + "请评估：1）是否适合校园发布 2）信息是否完整 3）建议通过还是驳回";
        }

        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", systemPrompt));
            msgs.add(Map.of("role", "user", "content", prompt));
            return callDeepSeek(msgs);
        } catch (Exception e) {
            return "AI 审核建议生成失败：" + e.getMessage();
        }
    }
}
