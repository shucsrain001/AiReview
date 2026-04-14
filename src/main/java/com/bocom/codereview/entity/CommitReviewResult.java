package com.bocom.codereview.entity;

import lombok.Data;

import java.util.List;

/**
 * 代码评审结果
 */
@Data
public class CommitReviewResult {
    private String projectName;
    private String commitId;
    private String author;
    private int criticalCount = 0;
    private int normalCount = 0;
    private int hintCount = 0;
    private List<CodeIssue> issues;
}