package com.example.newsapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.newsapp.service.NewsService;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    // Endpoint 1: Fetch from Web -> Filter -> Save to SQLite (Local Buffer)
    @GetMapping
    public Object fetchNews() {
        return newsService.fetchAndBufferNews();
    }

    // Endpoint 2: Read from SQLite -> Gemini -> Save to Supabase (Database)
    @GetMapping("/extract")
    public Object extractData() {
        return newsService.processPendingNews();
    }
}