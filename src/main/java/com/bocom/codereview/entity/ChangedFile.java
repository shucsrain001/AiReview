package com.bocom.codereview.entity;

import lombok.Data;

import java.util.List;

/**
 * 变更文件详情（含 AST 提取的上下文）
 */
@Data
public class ChangedFile {
    private String filePath;
    private String changeType; // ADD/MODIFY/DELETE
    private String rawDiff;
    private String context; // 核心：AST 提取的方法上下文或文本上下文
    private List<Integer> changedLines;
}