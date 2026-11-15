package com.acme.hrassistant.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Upload upload = new Upload();
    private final Rag rag = new Rag();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Upload getUpload() {
        return upload;
    }

    public Rag getRag() {
        return rag;
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86_400_000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://127.0.0.1:3000");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        /**
         * Accepts a YAML list or a single comma-separated value from
         * {@code APP_CORS_ALLOWED_ORIGINS} (typical in Azure / GitHub env secrets).
         */
        public void setAllowedOrigins(List<String> allowedOrigins) {
            if (allowedOrigins != null
                    && allowedOrigins.size() == 1
                    && allowedOrigins.getFirst().contains(",")) {
                this.allowedOrigins = Arrays.stream(allowedOrigins.getFirst().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                return;
            }
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Upload {
        /** Local mirror of policy files (readable even when Docker DB is down). */
        private String localDir = "./data/policies";
        private long maxBytes = 1_048_576L;
        private String allowedTypes = "text/markdown,text/plain";

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public String getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(String allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }

    public static class Rag {
        private int topK = 5;
        private double similarityThreshold = 0.45;
        private boolean seedOnEmpty = true;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public boolean isSeedOnEmpty() {
            return seedOnEmpty;
        }

        public void setSeedOnEmpty(boolean seedOnEmpty) {
            this.seedOnEmpty = seedOnEmpty;
        }
    }
}
