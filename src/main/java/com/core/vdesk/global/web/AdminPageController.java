package com.core.vdesk.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping({"", "/"})
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users() {
        return "admin/users/list";
    }

    @GetMapping("/payments")
    public String payments() {
        return "admin/payments/list";
    }

    @GetMapping("/devices")
    public String devices() {
        return "admin/devices/list";
    }

    @GetMapping("/visitors")
    public String visitors() {
        return "admin/visitors/list";
    }

    @GetMapping("/visitors/stats")
    public String visitorStats() {
        return "admin/visitors/list";
    }

    @GetMapping("/support/notices")
    public String notices() {
        return "admin/support/notices";
    }

    @GetMapping("/support/qna")
    public String qna() {
        return "admin/support/qna";
    }

    @GetMapping("/support/faq")
    public String faq() {
        return "admin/support/faq";
    }
}
