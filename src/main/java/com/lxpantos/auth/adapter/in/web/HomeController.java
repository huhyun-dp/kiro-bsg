package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER) != null
                ? "redirect:/members"
                : "redirect:/login";
    }

    @GetMapping("/members")
    public String members(HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        return "members";
    }
}
