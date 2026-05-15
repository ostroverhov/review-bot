package org.reviewbot.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.reviewbot.config.property.SlackBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SlackClient {

    private static final Logger log = LoggerFactory.getLogger(SlackClient.class);

    private final WebClient slackWebClient;
    private final SlackBotProperties slackBotProperties;

    public SlackClient(WebClient slackWebClient, SlackBotProperties slackBotProperties) {
        this.slackWebClient = slackWebClient;
        this.slackBotProperties = slackBotProperties;
    }

    public String postMessage(String channelId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("channel", channelId);
        body.put("text", text);
        return postAndExtractTs("/chat.postMessage", body);
    }

    public void postThreadMessage(String channelId, String threadTs, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("channel", channelId);
        body.put("thread_ts", threadTs);
        body.put("text", text);
        postAndExtractTs("/chat.postMessage", body);
    }

    public void addReaction(String channelId, String messageTs, String emojiName) {
        Map<String, Object> body = new HashMap<>();
        body.put("channel", channelId);
        body.put("timestamp", messageTs);
        body.put("name", emojiName);
        JsonNode root = postSlack("/reactions.add", body);
        if (!root.path("ok").asBoolean(false)) {
            String err = root.path("error").asText("unknown_error");
            if ("already_reacted".equals(err)) {
                log.debug("Slack reaction already present: {}", emojiName);
                return;
            }
            throw new IllegalStateException("Slack reactions.add failed: " + err);
        }
    }

    private String postAndExtractTs(String path, Map<String, Object> body) {
        JsonNode root = postSlack(path, body);
        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Slack " + path + " failed: " + root.path("error").asText("unknown_error"));
        }
        return root.path("ts").asText();
    }

    private JsonNode postSlack(String path, Map<String, Object> body) {
        String token = slackBotProperties.token();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Slack bot token is not configured");
        }
        return slackWebClient
                .post()
                .uri(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}
