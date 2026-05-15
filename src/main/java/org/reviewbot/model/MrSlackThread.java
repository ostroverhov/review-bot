package org.reviewbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.PrePersist;
import java.time.Instant;

@Entity
@Table(
        name = "mr_slack_thread",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_mr_slack_thread_project_iid_channel",
                        columnNames = {"gitlab_project_id", "mr_iid", "slack_channel_id"}))
public class MrSlackThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_project_id", nullable = false)
    private Long gitlabProjectId;

    @Column(name = "mr_iid", nullable = false)
    private int mrIid;

    @Column(name = "slack_channel_id", nullable = false, length = 32)
    private String slackChannelId;

    @Column(name = "root_message_ts", nullable = false, length = 32)
    private String rootMessageTs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected MrSlackThread() {}

    public MrSlackThread(Long gitlabProjectId, int mrIid, String slackChannelId, String rootMessageTs) {
        this.gitlabProjectId = gitlabProjectId;
        this.mrIid = mrIid;
        this.slackChannelId = slackChannelId;
        this.rootMessageTs = rootMessageTs;
    }

    public Long getId() {
        return id;
    }

    public Long getGitlabProjectId() {
        return gitlabProjectId;
    }

    public int getMrIid() {
        return mrIid;
    }

    public String getSlackChannelId() {
        return slackChannelId;
    }

    public String getRootMessageTs() {
        return rootMessageTs;
    }
}
