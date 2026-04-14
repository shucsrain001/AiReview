package com.bocom.codereview.service;


import com.bocom.codereview.entity.ChangedFile;
import com.bocom.codereview.entity.CodeIssue;
import com.bocom.codereview.entity.CommitReviewResult;
import com.bocom.codereview.entity.GitCommit;
import com.bocom.codereview.util.ContextExtractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 代码评审服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final OllamaChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 批量评审 Commit 列表
     */
    public List<CommitReviewResult> batchReview(String projectName, List<GitCommit> commits) {
        return commits.stream()
                .map(commit -> reviewSingleCommit(projectName, commit))
                .toList();
    }

    /**
     * 评审单个 Commit
     */
    private CommitReviewResult reviewSingleCommit(String projectName, GitCommit commit) {
        CommitReviewResult result = new CommitReviewResult();
        result.setProjectName(projectName);
        result.setCommitId(commit.getCommitId());
        result.setAuthor(commit.getAuthor());
        result.setIssues(new ArrayList<>());

        // 拼接 Prompt：文件路径 + 上下文 + Diff
        StringBuilder promptSb = new StringBuilder();
        for (ChangedFile file : commit.getChangedFiles()) {
            promptSb.append("【文件路径】：").append(file.getFilePath()).append("\n");
            promptSb.append("【变更类型】：").append(file.getChangeType()).append("\n");
            promptSb.append("【上下文】：\n").append(file.getContext()).append("\n");
            promptSb.append("【Diff 变更】：\n").append(file.getRawDiff()).append("\n\n");
        }

        // 构建 Prompt
        String promptTemplate = """
                你是资深企业级 Java 代码评审专家，严格遵守《阿里巴巴 Java 开发手册》、安全编码规范、性能优化规范。
                评审要求：
                1. 必须基于提供的上下文和 Diff 进行评审，不能仅凭单行代码判断
                2. 重点检查：空指针风险、SQL 注入、未关闭资源、循环内数据库操作、硬编码密钥、异常未捕获、代码规范问题
                3. 输出格式为 JSON 数组，每个元素字段：level(CRITICAL/NORMAL/HINT)、filePath、lineNum、problem、reason、solution
                4. 没有问题时输出空数组，不要输出其他内容

                评审内容：
                {commit_context}
                提交作者：{author}
                提交信息：{message}
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("commit_context", promptSb.toString());
        params.put("author", commit.getAuthor());
        params.put("message", commit.getMessage());

        Prompt prompt = new PromptTemplate(promptTemplate).create(params);
        org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
        String aiResponse = response.getResult().getOutput().getContent();

        // 解析 AI 返回结果
        try {
            List<CodeIssue> issues = objectMapper.readValue(aiResponse, new TypeReference<List<CodeIssue>>() {});
            result.setIssues(issues);
            // 统计问题级别
            result.setCriticalCount((int) issues.stream().filter(issue -> "CRITICAL".equals(issue.getLevel())).count());
            result.setNormalCount((int) issues.stream().filter(issue -> "NORMAL".equals(issue.getLevel())).count());
            result.setHintCount((int) issues.stream().filter(issue -> "HINT".equals(issue.getLevel())).count());
        } catch (Exception e) {
            log.error("解析 AI 评审结果失败", e);
            CodeIssue errorIssue = new CodeIssue();
            errorIssue.setLevel("NORMAL");
            errorIssue.setFilePath("系统错误");
            errorIssue.setProblem("AI 结果解析失败");
            errorIssue.setReason(e.getMessage());
            result.getIssues().add(errorIssue);
        }
        return result;
    }
}