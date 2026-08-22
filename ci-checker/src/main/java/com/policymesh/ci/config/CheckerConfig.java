package com.policymesh.ci.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for the CI checker.
 * 
 * CLI arguments override environment variables.
 * Environment variables override defaults.
 */
public class CheckerConfig {

    // Defaults
    private static final String DEFAULT_POLICY_DIR = "./policies";
    private static final String DEFAULT_SERVICES_FILE = "./examples/services.json";
    private static final String DEFAULT_DATAFLOWS_FILE = "./examples/dataflows.json";
    private static final String DEFAULT_OUTPUT_FORMAT = "console";

    private Path policyDir;
    private Path servicesFile;
    private Path dataflowsFile;
    private String outputFormat;
    private boolean noColor;
    private boolean strictMode;
    private String backendUrl;
    private String apiToken;
    private boolean failOnWarning;

    public CheckerConfig() {
        this.policyDir = Paths.get(envOrDefault("POLICY_DIR", DEFAULT_POLICY_DIR));
        this.servicesFile = Paths.get(envOrDefault("SERVICES_FILE", DEFAULT_SERVICES_FILE));
        this.dataflowsFile = Paths.get(envOrDefault("DATAFLOWS_FILE", DEFAULT_DATAFLOWS_FILE));
        this.outputFormat = envOrDefault("OUTPUT_FORMAT", DEFAULT_OUTPUT_FORMAT);
        this.noColor = Boolean.parseBoolean(envOrDefault("NO_COLOR", "false"));
        this.strictMode = Boolean.parseBoolean(envOrDefault("STRICT", "false"));
        this.backendUrl = envOrDefault("POLICYMESH_BACKEND_URL", "");
        this.apiToken = envOrDefault("POLICYMESH_API_TOKEN", "");
        this.failOnWarning = Boolean.parseBoolean(envOrDefault("FAIL_ON_WARNING", "true"));
    }

    public Path getPolicyDir() { return policyDir; }
    public void setPolicyDir(Path policyDir) { this.policyDir = policyDir; }

    public Path getServicesFile() { return servicesFile; }
    public void setServicesFile(Path servicesFile) { this.servicesFile = servicesFile; }

    public Path getDataflowsFile() { return dataflowsFile; }
    public void setDataflowsFile(Path dataflowsFile) { this.dataflowsFile = dataflowsFile; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public boolean isNoColor() { return noColor; }
    public void setNoColor(boolean noColor) { this.noColor = noColor; }

    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }

    public String getBackendUrl() { return backendUrl; }
    public void setBackendUrl(String backendUrl) { this.backendUrl = backendUrl; }

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public boolean isFailOnWarning() { return failOnWarning; }
    public void setFailOnWarning(boolean failOnWarning) { this.failOnWarning = failOnWarning; }

    public boolean hasBackendUrl() {
        return backendUrl != null && !backendUrl.isBlank();
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    @Override
    public String toString() {
        return String.format("CheckerConfig{policyDir=%s, services=%s, dataflows=%s, output=%s, backend=%s}",
                policyDir, servicesFile, dataflowsFile, outputFormat,
                hasBackendUrl() ? "configured" : "offline");
    }
}
