package com.applyflow.jobcopilot.auth.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bootstrap.admin")
public class AdminBootstrapProperties {
    private boolean enabled = false;
    private String email = "";
    private String password = "";
    private boolean forcePasswordReset = false;
    private List<String> allowedProfiles = new ArrayList<>(List.of("staging", "dev"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isForcePasswordReset() {
        return forcePasswordReset;
    }

    public void setForcePasswordReset(boolean forcePasswordReset) {
        this.forcePasswordReset = forcePasswordReset;
    }

    public List<String> getAllowedProfiles() {
        return allowedProfiles;
    }

    public void setAllowedProfiles(List<String> allowedProfiles) {
        this.allowedProfiles = allowedProfiles == null ? List.of() : allowedProfiles;
    }
}
