package com.liatrio.dora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DoraApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context assembles without errors
    }
}
