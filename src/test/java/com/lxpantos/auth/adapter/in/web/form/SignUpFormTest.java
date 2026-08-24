package com.lxpantos.auth.adapter.in.web.form;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignUpFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsFormattedKoreanMobilePhoneNumber() {
        SignUpForm form = validForm();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        SignUpForm form = validForm();
        form.setPhoneNumber("02-1234-5678");

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phoneNumber"));
    }

    private SignUpForm validForm() {
        SignUpForm form = new SignUpForm();
        form.setName("홍길동");
        form.setEmail("user@example.com");
        form.setPhoneNumber("010-1234-5678");
        form.setPassword("password1");
        form.setConfirmPassword("password1");
        form.setTermsAccepted(true);
        return form;
    }
}
