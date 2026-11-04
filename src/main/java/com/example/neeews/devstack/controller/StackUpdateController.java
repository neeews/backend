package com.example.neeews.devstack.controller;

import com.example.neeews.devstack.dto.response.StackUpdateResponse;
import com.example.neeews.devstack.service.StackUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stack-updates")
@RequiredArgsConstructor
public class StackUpdateController {

    private final StackUpdateService stackUpdateService;

    @GetMapping
    public ResponseEntity<Map<String, List<StackUpdateResponse>>> getUpdates(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(Map.of("updates", stackUpdateService.getUpdates(type)));
    }
}
