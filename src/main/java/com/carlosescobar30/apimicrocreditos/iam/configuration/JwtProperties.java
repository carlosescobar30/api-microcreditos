package com.carlosescobar30.apimicrocreditos.iam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("security.jwt")
public record JwtProperties (String secretKey, Duration expiration) {
}
