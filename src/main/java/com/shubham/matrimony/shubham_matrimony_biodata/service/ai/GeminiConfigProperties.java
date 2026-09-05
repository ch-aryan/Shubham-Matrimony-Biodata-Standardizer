package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfigProperties {

    private Api api = new Api();
    private Routing routing = new Routing();

    @Data
    public static class Api {
        private boolean enabled = true;
        private String key = "";
        private String model = "gemini-2.5-flash";
        private int timeoutSeconds = 30;
        private int maxRetries = 2;
    }

    @Data
    public static class Routing {
        private int minFieldsThreshold = 8;
        private boolean alwaysCallIfUnparsed = true;
        private boolean alwaysCallIfConflict = true;
    }
}

