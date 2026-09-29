package com.carlosescobar30.apimicrocreditos.iam.configuration;

import com.carlosescobar30.apimicrocreditos.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminAccountProperties properties;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {

        if (!StringUtils.hasText(properties.username())
                || !StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.password())){

            log.info("No admin account configured");
            return;

        }

        userService.createAdminIfMissing(properties.username(), properties.email(), properties.password());

    }

}
