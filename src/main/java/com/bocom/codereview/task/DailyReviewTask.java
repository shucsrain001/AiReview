package com.bocom.codereview.task;


import com.bocom.codereview.entity.CommitReviewResult;
import com.bocom.codereview.entity.GitCommit;
import com.bocom.codereview.service.AiReviewService;
import com.bocom.codereview.service.GitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 每日自动评审定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReviewTask {

    @Value("${code-review.git-repo-paths:}")
    private List<String> gitRepoPaths;

    private final GitService gitService;
    private final AiReviewService aiReviewService;

    /**
     * 每天凌晨 2 点执行
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 44 9 * * ?")
    public void runDailyReview() {
        log.info("===== 开始每日代码评审 =====");
        // 目标日期：昨天
        Date yesterday = new Date(System.currentTimeMillis() - 1* 60 * 60 * 1000);

        for (String repoPath : gitRepoPaths) {
            String projectName = new java.io.File(repoPath).getName();
            log.info("开始评审项目：{}", projectName);

            // 1. 获取昨天的 Commit 列表（带上下文）
            List<GitCommit> commits = gitService.getDailyCommits(repoPath, yesterday);
            if (commits.isEmpty()) {
                log.info("项目 {} 昨天无 Commit", projectName);
                continue;
            }

            // 2. AI 评审
            List<CommitReviewResult> results = aiReviewService.batchReview(projectName, commits);

            // 3. 后续操作：保存结果 + 发送通知（可扩展）
            saveReviewResults(results);
            sendReviewNotifications(results);
        }
        log.info("===== 每日代码评审结束 =====");
    }

    /**
     * 保存评审结果（可替换为数据库存储）
     */
    private void saveReviewResults(List<CommitReviewResult> results) {
        // TODO: 保存到 MySQL/Redis
        results.forEach(result -> log.info("评审结果：{}", result));
    }

    /**
     * 发送通知（可扩展为飞书/钉钉机器人）
     */
    private void sendReviewNotifications(List<CommitReviewResult> results) {
        // TODO: 调用飞书/钉钉 API 发送通知
    }
}