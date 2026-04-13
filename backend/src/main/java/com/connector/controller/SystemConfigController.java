package com.connector.controller;

import com.connector.entity.SystemConfig;
import com.connector.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository repository;

    @GetMapping("/sms")
    public Map<String, String> getSmsConfig() {
        List<SystemConfig> configs = repository.findAll();
        Map<String, String> result = new HashMap<>();
        // Defaults
        result.put("sms.url", "");
        result.put("sms.key.phone", "mobile");
        result.put("sms.key.content", "content");

        for (SystemConfig config : configs) {
            if (config.getConfigKey().startsWith("sms.")) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return result;
    }

    @PostMapping("/sms")
    public void saveSmsConfig(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            if (entry.getKey().startsWith("sms.")) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                repository.save(config);
            }
        }
    }
}
