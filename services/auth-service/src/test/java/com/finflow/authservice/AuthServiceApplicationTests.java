package com.finflow.authservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot context load test.
 * Disabled by default if external MySQL database infrastructure (DB_PASSWORD / MySQL instance) is unavailable.
 */
@Disabled("Requires running MySQL instance with finflow_auth database and DB_PASSWORD environment variable")
@SpringBootTest
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
