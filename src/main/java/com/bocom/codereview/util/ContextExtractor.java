package com.bocom.codereview.util;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * AST 上下文提取工具（Java文件） + 文本上下文提取（非Java文件）
 */
@Slf4j
@Component
public class ContextExtractor {

    @Value("${code-review.non-java-context-lines:5}")
    private int nonJavaContextLines;

    /**
     * 提取文件上下文（自动区分 Java/非Java）
     */
    public String extractContext(String filePath, List<Integer> changedLines) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "文件不存在";
        }

        // Java 文件：AST 提取方法上下文
        if (filePath.endsWith(".java")) {
            return extractJavaMethodContext(file, changedLines);
        }
        // 非 Java 文件：提取变更行前后 N 行文本
        else {
            return extractTextContext(file, changedLines);
        }
    }

    /**
     * Java 文件：AST 解析 → 提取变更行所在的完整方法
     */
    private String extractJavaMethodContext(File javaFile, List<Integer> changedLines) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            StringBuilder contextSb = new StringBuilder();

            for (int line : changedLines) {
                MethodDeclaration method = findMethodByLine(cu, line);
                if (method != null) {
                    contextSb.append("=== 匹配方法（行号：").append(line).append("）===\n");
                    contextSb.append(method.toString()).append("\n\n");
                } else {
                    contextSb.append("=== 行号 ").append(line).append(" 未匹配到方法 ===\n");
                }
            }
            return contextSb.toString();
        } catch (Exception e) {
            log.error("AST 解析失败", e);
            return "AST 解析失败：" + e.getMessage();
        }
    }

    /**
     * 核心：根据行号查找方法
     */
    private MethodDeclaration findMethodByLine(CompilationUnit cu, int targetLine) {
        final MethodDeclaration[] foundMethod = {null};
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);
                if (md.getRange().isPresent()) {
                    int start = md.getRange().get().begin.line;
                    int end = md.getRange().get().end.line;
                    if (targetLine >= start && targetLine <= end) {
                        foundMethod[0] = md;
                    }
                }
            }
        }, null);
        return foundMethod[0];
    }

    /**
     * 非 Java 文件：提取变更行前后 N 行文本
     */
    private String extractTextContext(File file, List<Integer> changedLines) {
        try {
            List<String> allLines = Files.readAllLines(file.toPath());
            StringBuilder contextSb = new StringBuilder();

            for (int line : changedLines) {
                // 行号转索引（文件行号从1开始，List从0开始）
                int lineIndex = line - 1;
                if (lineIndex < 0 || lineIndex >= allLines.size()) {
                    continue;
                }
                // 计算上下文范围
                int start = Math.max(0, lineIndex - nonJavaContextLines);
                int end = Math.min(allLines.size() - 1, lineIndex + nonJavaContextLines);

                contextSb.append("=== 文本上下文（行号：").append(line).append("）===\n");
                for (int i = start; i <= end; i++) {
                    contextSb.append((i + 1)).append(": ").append(allLines.get(i)).append("\n");
                }
                contextSb.append("\n");
            }
            return contextSb.toString();
        } catch (Exception e) {
            log.error("文本上下文提取失败", e);
            return "文本提取失败：" + e.getMessage();
        }
    }
}