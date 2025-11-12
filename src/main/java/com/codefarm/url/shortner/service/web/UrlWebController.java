package com.codefarm.url.shortner.service.web;

import com.codefarm.url.shortner.service.core.UrlShortenerService;
import com.codefarm.url.shortner.service.web.dto.ShortenRequest;
import com.codefarm.url.shortner.service.web.dto.ShortenResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class UrlWebController {

    private static final String VIEW_INDEX = "index";

    private final UrlShortenerService service;

    public UrlWebController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index() {
        return VIEW_INDEX;
    }

    @PostMapping("/shorten")
    public String shorten(@RequestParam("longUrl") String longUrl,
                          @RequestParam(value = "customAlias", required = false) String customAlias,
                          @RequestParam(value = "userUuid", required = false) String userUuid,
                          HttpServletRequest request,
                          Model model) {
        String baseUrl = getBaseUrl(request);
        ShortenResponse response = service.shortenUrl(new ShortenRequest(longUrl, customAlias), baseUrl, userUuid);
        model.addAttribute("shortUrl", response.shortUrl());
        model.addAttribute("shortCode", response.shortCode());
        model.addAttribute("createdAt", response.createdAt());
        return VIEW_INDEX;
    }

    private static String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefault = (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
        return scheme + "://" + host + (isDefault ? "" : (":" + port));
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return VIEW_INDEX;
    }
}


