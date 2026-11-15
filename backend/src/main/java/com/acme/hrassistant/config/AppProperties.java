package com.acme.hrassistant.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Upload upload = new Upload();
    private final Rag rag = new Rag();
    private final Agent agent = new Agent();
    private final Actions actions = new Actions();
    private final RateLimit ratelimit = new RateLimit();
    private final Cache cache = new Cache();
    private final Llm llm = new Llm();
    private final Chat chat = new Chat();

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

    public Agent getAgent() {
        return agent;
    }

    public Actions getActions() {
        return actions;
    }

    public RateLimit getRatelimit() {
        return ratelimit;
    }

    public Cache getCache() {
        return cache;
    }

    public Llm getLlm() {
        return llm;
    }

    public Chat getChat() {
        return chat;
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
        private long maxBytes = 5_242_880L;
        private String allowedTypes = com.acme.hrassistant.rag.PolicyUploadFormats.DEFAULT_ALLOWED_TYPES;

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

    public static class Agent {
        private int maxToolIterations = 8;

        public int getMaxToolIterations() {
            return maxToolIterations;
        }

        public void setMaxToolIterations(int maxToolIterations) {
            this.maxToolIterations = maxToolIterations;
        }
    }

    public static class Actions {
        private long pendingTtlMinutes = 30;

        public long getPendingTtlMinutes() {
            return pendingTtlMinutes;
        }

        public void setPendingTtlMinutes(long pendingTtlMinutes) {
            this.pendingTtlMinutes = pendingTtlMinutes;
        }
    }

    public static class RateLimit {
        private int loginPerMinute = 10;
        private int chatPerMinute = 20;
        private int policyWritePerMinute = 5;

        public int getLoginPerMinute() {
            return loginPerMinute;
        }

        public void setLoginPerMinute(int loginPerMinute) {
            this.loginPerMinute = loginPerMinute;
        }

        public int getChatPerMinute() {
            return chatPerMinute;
        }

        public void setChatPerMinute(int chatPerMinute) {
            this.chatPerMinute = chatPerMinute;
        }

        public int getPolicyWritePerMinute() {
            return policyWritePerMinute;
        }

        public void setPolicyWritePerMinute(int policyWritePerMinute) {
            this.policyWritePerMinute = policyWritePerMinute;
        }
    }

    public static class Cache {
        private Duration policiesTtl = Duration.ofMinutes(5);
        private Duration holidaysTtl = Duration.ofHours(1);

        public Duration getPoliciesTtl() {
            return policiesTtl;
        }

        public void setPoliciesTtl(Duration policiesTtl) {
            this.policiesTtl = policiesTtl;
        }

        public Duration getHolidaysTtl() {
            return holidaysTtl;
        }

        public void setHolidaysTtl(Duration holidaysTtl) {
            this.holidaysTtl = holidaysTtl;
        }
    }

    public static class Llm {
        /** Gemini HTTP timeout in milliseconds (Google GenAI HttpOptions). */
        private int readTimeoutMs = 60_000;

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Chat {
        private int maxMessageLength = 4000;

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }
    }
}
