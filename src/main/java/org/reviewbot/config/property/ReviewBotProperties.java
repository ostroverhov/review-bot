package org.reviewbot.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "review-bot")
public record ReviewBotProperties(
        String gitlabWebhookSecret, String closeReactionEmoji, long teamRulesRefreshIntervalMs) {}
