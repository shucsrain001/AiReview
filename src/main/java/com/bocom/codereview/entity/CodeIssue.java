package com.bocom.codereview.entity;

import lombok.Data;

/**
 * 具体问题
 */
@Data
public class CodeIssue {
    private String level; // CRITICAL/NORMAL/HINT
    private String filePath;
    private int lineNum;
    private String problem;
    private String reason;
    private String solution;
}