package org.reviewbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamRule(String gitlabProject, String slackChannelId) {}
