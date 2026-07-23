package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.entity.SystemConfig;
import com.example.aftersight.mapper.ConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigMapper configMapper;

    public ConfigController(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @GetMapping("/list")
    public Result<Map<String, List<SystemConfig>>> list(@RequestParam(required = false) String group) {
        List<SystemConfig> list = group != null ? configMapper.selectByGroup(group) : configMapper.selectAll();
        Map<String, List<SystemConfig>> grouped = list.stream()
                .collect(Collectors.groupingBy(SystemConfig::getConfigGroup, LinkedHashMap::new, Collectors.toList()));
        return Result.success(grouped);
    }

    @PutMapping("/update")
    public Result update(@RequestBody SystemConfig config) {
        if (config.getId() == null) return Result.fail(400, "ID不能为空");
        int rows = configMapper.updateValue(config.getId(), config.getConfigValue());
        if (rows == 0) return Result.fail(404, "配置不存在");
        return Result.success("配置更新成功，已实时生效", config);
    }

    @PutMapping("/batch-update")
    public Result batchUpdate(@RequestBody Map<String, List<Map<String, Object>>> body) {
        List<Map<String, Object>> configs = body.get("configs");
        if (configs == null || configs.isEmpty()) return Result.fail(400, "配置列表不能为空");
        int success = 0;
        for (Map<String, Object> item : configs) {
            int rows = configMapper.updateValue(((Number) item.get("id")).longValue(), (String) item.get("configValue"));
            if (rows > 0) success++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failCount", configs.size() - success);
        return Result.success(success + " 条配置更新成功", result);
    }

    @PostMapping("/reset/{id}")
    public Result reset(@PathVariable Long id) {
        SystemConfig config = configMapper.selectById(id);
        if (config == null) return Result.fail(404, "配置不存在");
        if ("redis".equals(config.getConfigGroup())) {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("rate_limit.max_requests_per_min", "5");
            defaults.put("rate_limit.token_bucket_capacity", "100");
            defaults.put("cache.qa_ttl_seconds", "300");
            defaults.put("rate_limit.enabled", "true");
            String defaultValue = defaults.get(config.getConfigKey());
            if (defaultValue != null) {
                configMapper.updateValue(id, defaultValue);
            }
        }
        return Result.success("配置已恢复为默认值");
    }
}
