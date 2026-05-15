package org.reviewbot.service;

import org.reviewbot.teams.TeamRegistryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TeamRuleRefreshScheduler {

    private final TeamRegistryService teamRegistryService;

    public TeamRuleRefreshScheduler(TeamRegistryService teamRegistryService) {
        this.teamRegistryService = teamRegistryService;
    }

    @Scheduled(fixedRateString = "${review-bot.team-rules-refresh-interval-ms}")
    public void refreshTeamRules() {
        teamRegistryService.reload();
    }
}
