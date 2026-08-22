package com.carlosescobar30.apimicrocreditos.iam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("security.refresh-token")
public record RefreshTokenProperties(Duration expiration, Duration gracePeriod) {
}
