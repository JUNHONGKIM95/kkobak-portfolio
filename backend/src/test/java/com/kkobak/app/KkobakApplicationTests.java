package com.kkobak.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.admin.password=test-admin-password")
class KkobakApplicationTests {
    @Test void contextLoads() { }
}
