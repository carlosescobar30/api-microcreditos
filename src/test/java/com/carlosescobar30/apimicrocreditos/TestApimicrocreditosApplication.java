package com.carlosescobar30.apimicrocreditos;

import org.springframework.boot.SpringApplication;

public class TestApimicrocreditosApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApimicrocreditosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
