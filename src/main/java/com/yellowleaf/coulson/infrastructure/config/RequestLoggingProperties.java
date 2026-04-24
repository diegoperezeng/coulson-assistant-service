package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "request-logging")
public class RequestLoggingProperties {

    private boolean enabled = true;
    private List<String> ignorePrefixes = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/info"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIgnorePrefixes() {
        return ignorePrefixes;
    }

    public void setIgnorePrefixes(List<String> ignorePrefixes) {
        this.ignorePrefixes = ignorePrefixes;
    }
}

