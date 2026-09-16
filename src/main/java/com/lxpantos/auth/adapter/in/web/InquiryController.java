package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.InquiryForm;
import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryAttachmentNotFoundException;
import com.lxpantos.auth.application.exception.InquiryAttachmentStorageException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.exception.InvalidInquiryAttachmentException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryUseCase;
import com.lxpantos.auth.application.port.in.DownloadInquiryAttachmentUseCase;
import com.lxpantos.auth.application.port.in.InquiryAttachmentDownload;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import com.lxpantos.auth.application.port.in.PendingInquiryAttachment;
import com.lxpantos.auth.application.port.in.UpdateInquiryCommand;
import com.lxpantos.auth.application.port.in.UpdateInquiryUseCase;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/inquiries")
public class InquiryController {
    private static final int PAGE_SIZE = 10;
    private final CreateInquiryUseCase createInquiryUseCase;
    private final UpdateInquiryUseCase updateInquiryUseCase;
    private final DeleteInquiryUseCase deleteInquiryUseCase;
    private final DeleteInquiryAttachmentUseCase deleteInquiryAttachmentUseCase;
    private final InquiryQueryUseCase inquiryQueryUseCase;
    private final InquiryAttachmentUploadValidator attachmentValidator;
    private final DownloadInquiryAttachmentUseCase downloadUseCase;
    private final InquiryAttachmentStorage attachmentStorage;

    public InquiryController(CreateInquiryUseCase createInquiryUseCase, UpdateInquiryUseCase updateInquiryUseCase,
                             DeleteInquiryUseCase deleteInquiryUseCase, InquiryQueryUseCase inquiryQueryUseCase) {
        this(createInquiryUseCase, updateInquiryUseCase, deleteInquiryUseCase, null, inquiryQueryUseCase,
                new InquiryAttachmentUploadValidator(), null, null);
    }
    @Autowired
    public InquiryController(CreateInquiryUseCase createInquiryUseCase, UpdateInquiryUseCase updateInquiryUseCase,
                             DeleteInquiryUseCase deleteInquiryUseCase, DeleteInquiryAttachmentUseCase deleteInquiryAttachmentUseCase,
                             InquiryQueryUseCase inquiryQueryUseCase,
                             InquiryAttachmentUploadValidator attachmentValidator, DownloadInquiryAttachmentUseCase downloadUseCase,
                             InquiryAttachmentStorage attachmentStorage) {
        this.createInquiryUseCase = createInquiryUseCase; this.updateInquiryUseCase = updateInquiryUseCase;
        this.deleteInquiryUseCase = deleteInquiryUseCase; this.deleteInquiryAttachmentUseCase = deleteInquiryAttachmentUseCase;
        this.inquiryQueryUseCase = inquiryQueryUseCase;
        this.attachmentValidator = attachmentValidator; this.downloadUseCase = downloadUseCase; this.attachmentStorage = attachmentStorage;
    }
    @GetMapping public String list(@RequestParam(defaultValue = "1") int page, HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        model.addAttribute("inquiryPage", inquiryQueryUseCase.getPage(page, PAGE_SIZE)); return "inquiry/list";
    }
    @GetMapping("/new") public String newForm(HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        if (!model.containsAttribute("inquiryForm")) model.addAttribute("inquiryForm", new InquiryForm());
        return "inquiry/form";
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String create(@Valid InquiryForm inquiryForm, BindingResult bindingResult, HttpSession session, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                SessionMember member = currentMember(session);
                Long id = createInquiryUseCase.create(new CreateInquiryCommand(member.id(), inquiryForm.getTitle(), inquiryForm.getContent(),
                        attachmentValidator.validate(inquiryForm.getAttachments())));
                return "redirect:/inquiries/" + id;
            } catch (com.lxpantos.auth.application.exception.InvalidInquiryAttachmentException e) {
                bindingResult.rejectValue("attachments", "invalid", e.getMessage());
            } catch (InquiryAttachmentStorageException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다.");
            }
        }
        model.addAttribute("member", session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER));
        model.addAttribute("formMode", "create"); return "inquiry/form";
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id, HttpSession session, Model model) {
        try { SessionMember member = currentMember(session); model.addAttribute("member", member);
            Long viewerMemberId = member != null ? member.id() : null;
            model.addAttribute("inquiry", inquiryQueryUseCase.getDetail(id, viewerMemberId)); model.addAttribute("canManage", isAuthor(id, member)); return "inquiry/detail";
        } catch (InquiryNotFoundException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); }
    }
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        SessionMember member = currentMember(session);
        requireAuthor(id, member);
        return populateEditForm(id, member, null, model);
    }
    @PostMapping(value = "/{id}/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String update(@PathVariable Long id, @Valid InquiryForm inquiryForm, BindingResult bindingResult,
                         HttpSession session, Model model) {
        SessionMember member = currentMember(session);
        requireAuthor(id, member);
        InquiryDetail detail;
        try {
            detail = inquiryQueryUseCase.getDetailWithoutView(id);
        } catch (InquiryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }

        if (bindingResult.hasErrors()) {
            return populateEditForm(id, member, inquiryForm, detail, model);
        }

        try {
            List<PendingInquiryAttachment> newAttachments = validateEditAttachments(detail, inquiryForm);
            updateInquiryUseCase.update(new UpdateInquiryCommand(
                    id, member.id(), false, inquiryForm.getTitle(), inquiryForm.getContent(),
                    newAttachments));
            return "redirect:/inquiries/" + id;
        } catch (InvalidInquiryAttachmentException e) {
            bindingResult.reject("inquiry.update", e.getMessage());
            return populateEditForm(id, member, inquiryForm, detail, model);
        } catch (InquiryAttachmentStorageException e) {
            bindingResult.reject("inquiry.update", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            return populateEditForm(id, member, inquiryForm, detail, model);
        } catch (InquiryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (InquiryAccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
    @PostMapping("/{id}/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId, HttpSession session) {
        SessionMember member = currentMember(session);
        requireAuthor(id, member);
        try {
            deleteInquiryAttachmentUseCase.deleteAttachment(new DeleteInquiryAttachmentCommand(id, attachmentId, member.id()));
            return "redirect:/inquiries/" + id + "/edit";
        } catch (InquiryAttachmentNotFoundException | InquiryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다.");
        } catch (InquiryAccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id, HttpSession session) {
        SessionMember member = currentMember(session);
        try { deleteInquiryUseCase.delete(new DeleteInquiryCommand(id, member.id(), member.isAdmin())); return "redirect:/inquiries";
        } catch (InquiryNotFoundException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); }
        catch (InquiryAccessDeniedException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); }
    }
    @GetMapping("/{inquiryId}/attachments/{attachmentId}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long inquiryId, @PathVariable Long attachmentId) {
        try { InquiryAttachmentDownload attachment = downloadUseCase.prepareDownload(inquiryId, attachmentId);
            var input = attachmentStorage.open(attachment.storageKey());
            StreamingResponseBody body = output -> { try (input) { input.transferTo(output); } };
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.mediaType().contentType())).contentLength(attachment.fileSize())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(attachment.originalFilename(), StandardCharsets.UTF_8).build().toString())
                    .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(body);
        } catch (InquiryAttachmentNotFoundException | IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."); }
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Void> uploadTooLarge() { return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build(); }
    private SessionMember currentMember(HttpSession session) { return (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER); }

    private boolean isAuthor(Long inquiryId, SessionMember member) {
        return member != null && inquiryQueryUseCase.findOwnerId(inquiryId)
                .map(ownerId -> ownerId.equals(member.id()))
                .orElse(false);
    }

    private void requireAuthor(Long inquiryId, SessionMember member) {
        Long ownerId = inquiryQueryUseCase.findOwnerId(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."));
        if (member == null || !ownerId.equals(member.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
    }

    private List<PendingInquiryAttachment> validateEditAttachments(InquiryDetail detail, InquiryForm inquiryForm) {
        // The edit POST no longer deletes attachments; count/size limits are evaluated against
        // the attachments currently stored on the inquiry plus the newly uploaded files.
        var currentAttachments = detail.attachments();
        long currentSize = currentAttachments.stream().mapToLong(attachment -> attachment.fileSize()).sum();
        return attachmentValidator.validate(inquiryForm.getAttachments(), currentAttachments.size(), currentSize);
    }

    private String populateEditForm(Long id, SessionMember member, InquiryForm inquiryForm, Model model) {
        try {
            return populateEditForm(id, member, inquiryForm, inquiryQueryUseCase.getDetailWithoutView(id), model);
        } catch (InquiryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    private String populateEditForm(Long id, SessionMember member, InquiryForm inquiryForm, InquiryDetail detail, Model model) {
        model.addAttribute("member", member);
        model.addAttribute("formMode", "edit");
        model.addAttribute("inquiryId", id);
        model.addAttribute("inquiry", detail);
        model.addAttribute("existingAttachments", detail.attachments());
        if (inquiryForm == null && !model.containsAttribute("inquiryForm")) {
            InquiryForm form = new InquiryForm();
            form.setTitle(detail.title());
            form.setContent(detail.content());
            model.addAttribute("inquiryForm", form);
        } else if (inquiryForm != null) {
            model.addAttribute("inquiryForm", inquiryForm);
        }
        return "inquiry/form";
    }
}
