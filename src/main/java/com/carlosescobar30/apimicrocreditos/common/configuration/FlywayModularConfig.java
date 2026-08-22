package com.carlosescobar30.apimicrocreditos.common.configuration;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@RequiredArgsConstructor
@Configuration
public class FlywayModularConfig {

    private final DataSource dataSource;

    @Bean(initMethod = "migrate", name = "iamFlyway")
    public Flyway iamFlyway() {

        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("iam")
                .locations("classpath:db/migration/iam")
                .load();

    }

    @Bean(initMethod = "migrate", name = "operationalFlyway")
    public Flyway operationalFlyway() {

        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("operational")
                .locations("classpath:db/migration/operational")
                .load();


    }

    @Bean
    public static BeanFactoryPostProcessor FlywayDependencies() {

        return beanFactory -> {
            String[] jpaBeanNames = beanFactory
                    .getBeanNamesForType(EntityManagerFactory.class);
            for (String beanNames : jpaBeanNames){
                BeanDefinition bd = beanFactory.getBeanDefinition(beanNames);
                bd.setDependsOn("iamFlyway","operationalFlyway");
            }
        };

    }

}
