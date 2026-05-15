package org.reviewbot.repository;

import java.util.List;
import java.util.Optional;

import org.reviewbot.model.MrSlackThread;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrSlackThreadRepository extends JpaRepository<MrSlackThread, Long> {

    Optional<MrSlackThread> findByGitlabProjectIdAndMrIidAndSlackChannelId(
            long gitlabProjectId, int mrIid, String slackChannelId);

    List<MrSlackThread> findAllByGitlabProjectIdAndMrIid(long gitlabProjectId, int mrIid);
}
