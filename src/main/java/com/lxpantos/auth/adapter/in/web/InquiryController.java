package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.InquiryForm;
import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/inquiries")
public class InquiryController {

    private static final int PAGE_SIZE = 10;

    private final CreateInquiryUseCase createInquiryUseCase;
    private final InquiryQueryUseCase inquiryQueryUseCase;

    public InquiryController(CreateInquiryUseCase createInquiryUseCase,
                              InquiryQueryUseCase inquiryQueryUseCase) {
        this.createInquiryUseCase = createInquiryUseCase;
        this.inquiryQueryUseCase = inquiryQueryUseCase;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       HttpSession session,
                       Model model) {
        InquiryPage inquiryPage = inquiryQueryUseCase.getPage(page, PAGE_SIZE);
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        model.addAttribute("inquiryPage", inquiryPage);
        return "inquiry/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        if (!model.containsAttribute("inquiryForm")) {
            model.addAttribute("inquiryForm", new InquiryForm());
        }
        return "inquiry/form";
    }

    @PostMapping
    public String create(@Valid InquiryForm inquiryForm,
                         BindingResult bindingResult,
                         HttpSession session,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
            return "inquiry/form";
        }

        SessionMember sessionMember =
                (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER);

        Long id = createInquiryUseCase.create(new CreateInquiryCommand(
                sessionMember.id(),
                inquiryForm.getTitle(),
                inquiryForm.getContent()
        ));

        return "redirect:/inquiries/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        try {
            model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
            model.addAttribute("inquiry", inquiryQueryUseCase.getDetail(id));
            return "inquiry/detail";
        } catch (InquiryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
