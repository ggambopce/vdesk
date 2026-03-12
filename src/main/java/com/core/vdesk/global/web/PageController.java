package com.core.vdesk.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf 페이지 라우팅 컨트롤러
 */
@Controller
public class PageController {

    // ── 랜딩 페이지 (공개) ──────────────────────────────
    @GetMapping("/")
    public String index() {
        return "landing/index";
    }

    @GetMapping("/intro")
    public String intro() {
        return "landing/intro";
    }

    @GetMapping("/method")
    public String method() {
        return "landing/method";
    }

    @GetMapping("/support")
    public String support() {
        return "landing/support";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "landing/pricing";
    }

    @GetMapping("/usecase")
    public String usecase() {
        return "landing/usecase";
    }

    // ── 인증 페이지 ─────────────────────────────────────
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

    // ── 앱 페이지 (인증 필요) ────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "dashboard/mypage";
    }
}
