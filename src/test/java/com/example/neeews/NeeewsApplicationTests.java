package com.example.neeews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.scheduling.enabled=false")
class NeeewsApplicationTests {

    @Test
    void contextLoads() {
    }

}
