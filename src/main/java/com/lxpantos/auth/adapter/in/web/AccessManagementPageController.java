package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessManagementPageController {

    @GetMapping("/admin/access")
    public String accessManagement(HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        return "admin/access";
    }
}
