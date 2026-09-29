package com.carlosescobar30.apimicrocreditos.iam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.admin")
public record AdminAccountProperties (String username, String email, String password) {
}
