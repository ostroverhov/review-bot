package org.reviewbot.repository;

import org.reviewbot.model.TeamRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRuleRepository extends JpaRepository<TeamRuleEntity, Long> {}
