package com.example.backend_cms.modules.general.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
