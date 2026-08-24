(function () {
    const phoneInput = document.getElementById('phoneNumber');
    if (!phoneInput) {
        return;
    }

    phoneInput.addEventListener('input', function () {
        const digits = phoneInput.value.replace(/\D/g, '').slice(0, 11);
        if (digits.length <= 3) {
            phoneInput.value = digits;
            return;
        }
        if (digits.length <= 7) {
            phoneInput.value = `${digits.slice(0, 3)}-${digits.slice(3)}`;
            return;
        }
        phoneInput.value = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
    });
}());
