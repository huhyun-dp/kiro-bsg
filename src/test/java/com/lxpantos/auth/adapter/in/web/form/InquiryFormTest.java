package com.lxpantos.auth.adapter.in.web.form;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidForm() {
        InquiryForm form = validForm();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsTitleExceeding20Characters() {
        InquiryForm form = validForm();
        form.setTitle("가".repeat(21));

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void rejectsContentExceeding500Characters() {
        InquiryForm form = validForm();
        form.setContent("내".repeat(501));

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("content"));
    }

    @Test
    void rejectsBlankTitle() {
        InquiryForm form = validForm();
        form.setTitle("   ");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void rejectsBlankContent() {
        InquiryForm form = validForm();
        form.setContent("   ");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("content"));
    }

    @Test
    void acceptsTitleExactly20Characters() {
        InquiryForm form = validForm();
        form.setTitle("가".repeat(20));

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void acceptsContentExactly500Characters() {
        InquiryForm form = validForm();
        form.setContent("나".repeat(500));

        assertThat(validator.validate(form)).isEmpty();
    }

    private InquiryForm validForm() {
        InquiryForm form = new InquiryForm();
        form.setTitle("테스트 문의 제목");
        form.setContent("테스트 문의 내용입니다.");
        return form;
    }
}
