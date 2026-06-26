package com.example.neeews.bookmark.controller;

import com.example.neeews.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> addBookmark(@PathVariable Long id, Authentication authentication) {
        bookmarkService.addBookmark(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/bookmark")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long id, Authentication authentication) {
        bookmarkService.removeBookmark(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
