package com.connector.service;

import com.connector.dto.SqlResult;
import com.connector.entity.TaskValidationRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    public String validate(SqlResult result, List<TaskValidationRule> rules) {
        if (!result.isSuccess()) {
            return "Execution Failed: " + result.getMessage();
        }

        for (TaskValidationRule rule : rules) {
            String failure = checkRule(result, rule);
            if (failure != null) {
                return failure;
            }
        }
        return null; // Success
    }

    private String checkRule(SqlResult result, TaskValidationRule rule) {
        if (rule == null || rule.getRuleType() == null) {
            return null; // Skip invalid rules
        }
        
        String expected = rule.getExpectedValue() != null ? rule.getExpectedValue().trim() : "";
        
        switch (rule.getRuleType()) {
            case THRESHOLD_COUNT:
                // Assume first column of first row is count or numeric value
                if (result.getRows().isEmpty()) return "Empty Result for Threshold Check";
                try {
                    Object val = result.getRows().get(0).values().iterator().next();
                    if (val == null) return "Null value for Threshold Check";
                    
                    double count = Double.parseDouble(val.toString());
                    
                    // Simple parser for "> 10", "< 5", "= 3"
                    if (expected.startsWith(">")) {
                        double threshold = Double.parseDouble(expected.substring(1).trim());
                        if (!(count > threshold)) return "Threshold Failed: " + count + " is not > " + threshold;
                    } else if (expected.startsWith("<")) {
                        double threshold = Double.parseDouble(expected.substring(1).trim());
                        if (!(count < threshold)) return "Threshold Failed: " + count + " is not < " + threshold;
                    } else if (expected.startsWith("=")) {
                         double threshold = Double.parseDouble(expected.substring(1).trim());
                         if (count != threshold) return "Threshold Failed: " + count + " != " + threshold;
                    } else {
                         // Default to > if no operator
                         double threshold = Double.parseDouble(expected);
                         if (!(count > threshold)) return "Threshold Failed: " + count + " is not > " + threshold;
                    }
                } catch (Exception e) {
                    return "Threshold Parse Error: " + e.getMessage();
                }
                break;
            case RESULT_EMPTY:
                if (!result.getRows().isEmpty()) return "Expected Empty Result, but found " + result.getRows().size() + " rows";
                break;
            case RESULT_NOT_EMPTY:
                if (result.getRows().isEmpty()) return "Expected Non-Empty Result";
                break;
            case REGEX_MATCH:
                // Check if ANY value matches regex (Alert if MATCHED or NOT MATCHED? usually we alert if something BAD is found)
                // Let's assume: If regex matches, it is GOOD (Validation Pass). If not match, Validation Fail.
                // WAIT, "Validation Rule" usually defines "What is Expected/Good".
                // So if rule is REGEX_MATCH "Success", it means we EXPECT to find "Success". If not found -> Alert.
                
                Pattern p = Pattern.compile(expected);
                boolean match = false;
                for (Map<String, Object> row : result.getRows()) {
                    for (Object val : row.values()) {
                        if (val != null && p.matcher(val.toString()).find()) {
                            match = true;
                            break;
                        }
                    }
                    if (match) break;
                }
                if (!match) return "Regex " + expected + " not found in result";
                break;
                
            case CONTAINS_TEXT:
                boolean contains = false;
                for (Map<String, Object> row : result.getRows()) {
                     for (Object val : row.values()) {
                         if (val != null && val.toString().contains(expected)) {
                             contains = true;
                             break;
                         }
                     }
                     if (contains) break;
                }
                if (!contains) return "Text '" + expected + "' not found in result";
                break;
        }
        return null;
    }
}
