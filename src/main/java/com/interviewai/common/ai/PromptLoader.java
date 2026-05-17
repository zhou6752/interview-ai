package com.interviewai.common.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Prompt 模板加载器，从 classpath:prompts/*.st 读取模板
 */
@Component
public class PromptLoader {

    /**
     * 加载 prompt 模板
     * @param path classpath 相对路径，如 "prompts/resume-analysis-system.st"
     */
    public String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("加载 prompt 模板失败: " + path, e);
        }
    }

    /**
     * 加载并替换占位符 {0}, {1}, {2}...
     */
    public String loadAndFormat(String path, Object... args) {
        String template = load(path);
        if (args == null || args.length == 0) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }
}
