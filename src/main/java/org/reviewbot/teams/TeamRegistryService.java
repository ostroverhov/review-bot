package org.reviewbot.teams;

import jakarta.annotation.PostConstruct;

import java.util.List;

import org.reviewbot.model.TeamRule;
import org.reviewbot.model.TeamRuleEntity;
import org.reviewbot.repository.TeamRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TeamRegistryService {

    private static final Logger log = LoggerFactory.getLogger(TeamRegistryService.class);

    private final TeamRuleRepository teamRuleRepository;

    private volatile List<TeamRule> rules = List.of();

    public TeamRegistryService(TeamRuleRepository teamRuleRepository) {
        this.teamRuleRepository = teamRuleRepository;
    }

    @PostConstruct
    void init() {
        reload();
    }

    public void reload() {
        this.rules = loadRules();
    }

    public List<TeamRule> rulesForProject(String pathWithNamespace) {
        if (pathWithNamespace == null) {
            return List.of();
        }
        return rules.stream()
                .filter(r -> pathWithNamespace.equals(r.gitlabProject()))
                .toList();
    }

    public List<TeamRule> allRules() {
        return rules;
    }

    private List<TeamRule> loadRules() {
        List<TeamRule> loaded = teamRuleRepository.findAll().stream()
                .map(this::toRule)
                .filter(this::isValid)
                .toList();
        for (TeamRule rule : loaded) {
            log.info("Loaded team mapping -> project={}, channel={}", rule.gitlabProject(), rule.slackChannelId());
        }
        log.info("Loaded {} team rule(s) from database", loaded.size());
        return loaded;
    }

    private TeamRule toRule(TeamRuleEntity entity) {
        return new TeamRule(entity.getGitlabProject(), entity.getSlackChannelId());
    }

    private boolean isValid(TeamRule rule) {
        if (rule.gitlabProject() == null
                || rule.gitlabProject().isBlank()
                || rule.slackChannelId() == null
                || rule.slackChannelId().isBlank()) {
            log.warn(
                    "Skipping invalid team rule (missing fields): project={}, channel={}",
                    rule.gitlabProject(),
                    rule.slackChannelId());
            return false;
        }
        return true;
    }
}
