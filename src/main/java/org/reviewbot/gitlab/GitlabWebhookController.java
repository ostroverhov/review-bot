package org.reviewbot.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import org.reviewbot.config.property.ReviewBotProperties;
import org.reviewbot.service.GitlabWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/gitlab")
public class GitlabWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitlabWebhookController.class);

    private final ReviewBotProperties reviewBotProperties;
    private final GitlabWebhookService gitlabWebhookService;

    public GitlabWebhookController(ReviewBotProperties reviewBotProperties, GitlabWebhookService gitlabWebhookService) {
        this.reviewBotProperties = reviewBotProperties;
        this.gitlabWebhookService = gitlabWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String event,
            @RequestBody JsonNode body) {

        if (!gitlabTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (event == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            switch (event) {
                case "Merge Request Hook" -> gitlabWebhookService.handleMergeRequestHook(body);
                case "Note Hook" -> gitlabWebhookService.handleNoteHook(body);
                default -> log.debug("Ignoring GitLab event type {}", event);
            }
        } catch (Exception e) {
            log.error("Failed to process GitLab webhook event={}", event, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok().build();
    }

    private boolean gitlabTokenValid(String token) {
        String expected = reviewBotProperties.gitlabWebhookSecret();
        if (expected == null || expected.isBlank()) {
            log.warn("GITLAB_WEBHOOK_SECRET / review-bot.gitlab-webhook-secret is not set; rejecting webhooks");
            return false;
        }
        return expected.equals(token);
    }
}
