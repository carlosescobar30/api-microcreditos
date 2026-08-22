package com.carlosescobar30.apimicrocreditos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModulithArchitectureTests {

    @Test
    void verifyModules (){

        var modules = ApplicationModules.of(ApimicrocreditosApplication.class);

        modules.verify();

    }

}
