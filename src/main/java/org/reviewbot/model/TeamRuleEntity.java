package org.reviewbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "team_rule",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_team_rule_project_channel",
                        columnNames = {"gitlab_project", "slack_channel_id"}))
public class TeamRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_project", nullable = false, length = 512)
    private String gitlabProject;

    @Column(name = "slack_channel_id", nullable = false, length = 32)
    private String slackChannelId;

    protected TeamRuleEntity() {}

    public TeamRuleEntity(String gitlabProject, String slackChannelId) {
        this.gitlabProject = gitlabProject;
        this.slackChannelId = slackChannelId;
    }

    public Long getId() {
        return id;
    }

    public String getGitlabProject() {
        return gitlabProject;
    }

    public String getSlackChannelId() {
        return slackChannelId;
    }
}
