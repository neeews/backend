package com.example.neeews.visit.controller;

import com.example.neeews.visit.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/ping")
    public ResponseEntity<Void> ping() {
        visitService.recordVisit();
        return ResponseEntity.ok().build();
    }
}
