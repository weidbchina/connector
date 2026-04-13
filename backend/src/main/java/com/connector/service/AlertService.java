package com.connector.service;

import com.connector.entity.MonitorTask;
import com.connector.entity.SystemConfig;
import com.connector.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SystemConfigRepository configRepository;
    private final Map<Long, LocalDateTime> lastAlertTimeMap = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendAlert(MonitorTask task, String errorMsg, String details) {
        log.info("Preparing alert for task: {}", task.getName());
        if (!shouldSendAlert(task)) {
            log.info("Alert suppressed for task {} due to rate limiting (Interval: {} min).", task.getName(), task.getAlertIntervalMinutes());
            return;
        }

        String template = task.getAlertTemplate();
        if (template == null || template.isEmpty()) {
            template = "Alert for Task: ${taskName}. Error: ${error}. Time: ${time}";
        }

        String content = template
                .replace("${taskName}", task.getName())
                .replace("${error}", errorMsg)
                .replace("${time}", LocalDateTime.now().toString());

        // SMS Simulation
        log.info("SENDING SMS to {}: {}", task.getAlertPhoneNumbers(), content);
        
        // Send Real SMS
        sendSms(task.getAlertPhoneNumbers(), content);
        
        // Update last alert time
        lastAlertTimeMap.put(task.getId(), LocalDateTime.now());
    }

    private void sendSms(String phoneNumbers, String content) {
        if (phoneNumbers == null || phoneNumbers.trim().isEmpty()) {
            return;
        }

        // Fetch Config
        List<SystemConfig> configs = configRepository.findAll();
        Map<String, String> configMap = configs.stream().collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
        
        String url = configMap.get("sms.url");
        String phoneKey = configMap.getOrDefault("sms.key.phone", "mobile");
        String contentKey = configMap.getOrDefault("sms.key.content", "content");

        if (url == null || url.trim().isEmpty()) {
            log.warn("SMS URL not configured. Skipping SMS.");
            return;
        }

        String[] phones = phoneNumbers.split("[,;]");
        for (String phone : phones) {
            if (phone.trim().isEmpty()) continue;
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put(phoneKey, phone.trim());
                payload.put(contentKey, content);
                
                restTemplate.postForObject(url, payload, String.class);
                log.info("SMS sent to {} via {}", phone, url);
            } catch (Exception e) {
                log.error("Failed to send SMS to {}", phone, e);
            }
        }
    }

    private boolean shouldSendAlert(MonitorTask task) {
        LocalDateTime lastTime = lastAlertTimeMap.get(task.getId());
        if (lastTime == null) {
            return true;
        }
        long minutesSinceLast = ChronoUnit.MINUTES.between(lastTime, LocalDateTime.now());
        return minutesSinceLast >= (task.getAlertIntervalMinutes() != null ? task.getAlertIntervalMinutes() : 10);
    }
}
