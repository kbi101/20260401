package com.timelord;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class TimelordApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verifyModulith() {
        ApplicationModules modules = ApplicationModules.of(TimelordApplication.class);
        modules.verify();
        new Documenter(modules).writeDocumentation();
    }

}
