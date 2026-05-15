package org.reviewbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.reviewbot.config.property.ReviewBotProperties;
import org.reviewbot.model.MrSlackThread;
import org.reviewbot.repository.MrSlackThreadRepository;
import org.reviewbot.client.SlackClient;
import org.reviewbot.teams.TeamRegistryService;
import org.reviewbot.model.TeamRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitlabWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitlabWebhookService.class);

    private final TeamRegistryService teamRegistryService;
    private final MrSlackThreadRepository mrSlackThreadRepository;
    private final SlackClient slackClient;
    private final ReviewBotProperties reviewBotProperties;

    public GitlabWebhookService(
            TeamRegistryService teamRegistryService,
            MrSlackThreadRepository mrSlackThreadRepository,
            SlackClient slackClient,
            ReviewBotProperties reviewBotProperties) {
        this.teamRegistryService = teamRegistryService;
        this.mrSlackThreadRepository = mrSlackThreadRepository;
        this.slackClient = slackClient;
        this.reviewBotProperties = reviewBotProperties;
    }

    @Transactional
    public void handleMergeRequestHook(JsonNode body) {
        JsonNode project = body.path("project");
        long projectId = project.path("id").asLong(0L);
        String path = textOrNull(project.path("path_with_namespace"));
        if (projectId == 0L || path == null) {
            log.warn("Merge request hook missing project id or path");
            return;
        }
        List<TeamRule> teams = teamRegistryService.rulesForProject(path);
        if (teams.isEmpty()) {
            log.debug("No team mapping for project {}", path);
            return;
        }

        JsonNode attrs = body.path("object_attributes");
        String action = attrs.path("action").asText("");
        int iid = attrs.path("iid").asInt();
        String title = attrs.path("title").asText("");
        String mrUrl = firstNonBlank(attrs.path("url").asText(null), attrs.path("web_url").asText(null));

        switch (action) {
            case "open", "reopen" -> onMrOpened(projectId, iid, teams, title, mrUrl);
            case "close", "merge" -> onMrClosedOrMerged(projectId, iid, action, mrUrl);
            default -> log.debug("Ignoring merge request action {}", action);
        }
    }

    @Transactional
    public void handleNoteHook(JsonNode body) {
        JsonNode attrs = body.path("object_attributes");
        if (!"MergeRequest".equals(attrs.path("noteable_type").asText())) {
            return;
        }
        JsonNode project = body.path("project");
        long projectId = project.path("id").asLong(0L);
        String path = textOrNull(project.path("path_with_namespace"));
        if (projectId == 0L || path == null) {
            log.warn("Note hook missing project id or path");
            return;
        }
        if (teamRegistryService.rulesForProject(path).isEmpty()) {
            return;
        }

        int mrIid = attrs.path("noteable_iid").asInt();
        String noteBody = attrs.path("note").asText("");
        String noteUrl = firstNonBlank(attrs.path("url").asText(null), attrs.path("web_url").asText(null));
        String author = attrs.path("author").path("username").asText("someone");

        List<MrSlackThread> threads = mrSlackThreadRepository.findAllByGitlabProjectIdAndMrIid(projectId, mrIid);
        if (threads.isEmpty()) {
            log.debug("No Slack root for MR projectId={} iid={}, skipping note", projectId, mrIid);
            return;
        }

        String line = "_Comment from @" + author + ":_ " + escapeSlackMrkdwn(noteBody);
        if (noteUrl != null) {
            line += "\n<" + noteUrl + "|Open comment>";
        }

        for (MrSlackThread t : threads) {
            slackClient.postThreadMessage(t.getSlackChannelId(), t.getRootMessageTs(), line);
        }
    }

    private void onMrOpened(long projectId, int iid, List<TeamRule> teams, String title, String mrUrl) {
        for (TeamRule team : teams) {
            if (mrSlackThreadRepository
                    .findByGitlabProjectIdAndMrIidAndSlackChannelId(projectId, iid, team.slackChannelId())
                    .isPresent()) {
                log.debug("Root message already exists for MR {}:{} channel {}", projectId, iid, team.slackChannelId());
                continue;
            }
            String text = "*MR opened:* !" + iid + " — " + escapeSlackMrkdwn(title);
            if (mrUrl != null) {
                text += "\n<" + mrUrl + "|View merge request>";
            }
            String ts = slackClient.postMessage(team.slackChannelId(), text);
            mrSlackThreadRepository.save(new MrSlackThread(projectId, iid, team.slackChannelId(), ts));
        }
    }

    private void onMrClosedOrMerged(long projectId, int iid, String action, String mrUrl) {
        List<MrSlackThread> threads = mrSlackThreadRepository.findAllByGitlabProjectIdAndMrIid(projectId, iid);
        if (threads.isEmpty()) {
            return;
        }
        String state = "merge".equals(action) ? "merged" : "closed";
        String line = "*MR " + state + ":* !" + iid;
        if (mrUrl != null) {
            line += "\n<" + mrUrl + "|View merge request>";
        }
        String emoji = reviewBotProperties.closeReactionEmoji();
        for (MrSlackThread t : threads) {
            slackClient.postThreadMessage(t.getSlackChannelId(), t.getRootMessageTs(), line);
            slackClient.addReaction(t.getSlackChannelId(), t.getRootMessageTs(), emoji);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        String s = n.asText();
        return s.isBlank() ? null : s;
    }

    /** Minimal escaping for & and <> in user-controlled fragments inside mrkdwn. */
    private static String escapeSlackMrkdwn(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
