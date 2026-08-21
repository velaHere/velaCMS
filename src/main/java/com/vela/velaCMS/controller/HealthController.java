package com.vela.velaCMS.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/healthy-vela")
public class HealthController {
    @GetMapping()
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok().build();
    }
}
