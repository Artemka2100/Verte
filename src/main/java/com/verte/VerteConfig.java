package com.verte;

import net.minecraftforge.common.ForgeConfigSpec;

public class VerteConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.ConfigValue<String> API_KEY;
    private static final ForgeConfigSpec.ConfigValue<String> API_URL;
    private static final ForgeConfigSpec.ConfigValue<String> MODEL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Verte AI settings").push("verte");

        API_KEY = b
                .comment("API key for the chat-completions endpoint.",
                        "Free option: create a key at https://console.groq.com (Groq is free and fast).",
                        "You can also leave this empty and set the VERTE_API_KEY environment variable instead.")
                .define("apiKey", "");

        API_URL = b
                .comment("OpenAI-compatible chat-completions endpoint URL.",
                        "Default is Groq. Any OpenAI-compatible free endpoint works.")
                .define("apiUrl", "https://api.groq.com/openai/v1/chat/completions");

        MODEL = b
                .comment("Model name to use on that endpoint.")
                .define("model", "openai/gpt-oss-120b");

        b.pop();
        SPEC = b.build();
    }

    public static String apiKey() {
        String key = API_KEY.get();
        if (key == null || key.isBlank()) {
            String env = System.getenv("VERTE_API_KEY");
            if (env != null && !env.isBlank()) return env;
        }
        return key;
    }

    public static String apiUrl() {
        return API_URL.get();
    }

    public static String model() {
        return MODEL.get();
    }
}
