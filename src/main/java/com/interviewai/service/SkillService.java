package com.interviewai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public record SkillDefinition(String id, String name, String description,
                                   List<CategoryDefinition> categories) {
    }

    public record CategoryDefinition(String name, int weight, List<String> topics) {
    }

    @PostConstruct
    void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:skills/*.yml");
            Yaml yaml = new Yaml();

            for (Resource resource : resources) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = yaml.load(resource.getInputStream());
                if (map == null) continue;

                for (var entry : map.entrySet()) {
                    String id = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> skillData = (Map<String, Object>) entry.getValue();

                    String name = (String) skillData.get("name");
                    String description = (String) skillData.get("description");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cats = (List<Map<String, Object>>) skillData.get("categories");
                    List<CategoryDefinition> categoryDefs = new ArrayList<>();

                    if (cats != null) {
                        for (Map<String, Object> cat : cats) {
                            String catName = (String) cat.get("name");
                            int weight = cat.get("weight") instanceof Integer i ? i :
                                    Integer.parseInt(cat.get("weight").toString());
                            @SuppressWarnings("unchecked")
                            List<String> topics = (List<String>) cat.get("topics");
                            categoryDefs.add(new CategoryDefinition(catName, weight, topics));
                        }
                    }

                    skills.put(id, new SkillDefinition(id, name, description, categoryDefs));
                    log.info("加载面试方向: {} ({})", name, categoryDefs.size() + "个范畴");
                }
            }
        } catch (Exception e) {
            log.error("加载 Skill 配置文件失败", e);
            throw new RuntimeException("Skill 配置文件加载失败: " + e.getMessage(), e);
        }
    }

    public List<SkillDefinition> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }
}