(function () {
    'use strict';

    var API_BASE = '/api/admin/members';
    var PAGE_SIZE = 20;
    var AUDIT_PAGE_SIZE = 5;

    var csrfToken = (document.querySelector('meta[name="_csrf"]') || {}).content || '';

    var ROLE_LABEL = {ADMIN: 'ADMIN', OPERATOR: 'OPERATOR', VIEWER: 'VIEWER'};
    var STATUS_LABEL = {ACTIVE: '활성', SUSPENDED: '정지'};

    var state = {page: 0, totalPages: 1, keyword: '', role: '', status: ''};
    var audit = {memberId: null, page: 0, totalPages: 1};

    var els = {
        form: document.getElementById('accessSearchForm'),
        keyword: document.getElementById('accessKeyword'),
        role: document.getElementById('accessRole'),
        status: document.getElementById('accessStatus'),
        count: document.getElementById('accessCount'),
        statusMessage: document.getElementById('accessStatusMessage'),
        prev: document.getElementById('accessPrev'),
        next: document.getElementById('accessNext'),
        pageInfo: document.getElementById('accessPageInfo'),
        modal: document.getElementById('accessModal'),
        modalName: document.getElementById('accessModalName'),
        modalEmail: document.getElementById('accessModalEmail'),
        modalMemberId: document.getElementById('accessModalMemberId'),
        modalVersion: document.getElementById('accessModalVersion'),
        modalRole: document.getElementById('accessModalRole'),
        modalStatus: document.getElementById('accessModalStatus'),
        modalReason: document.getElementById('accessModalReason'),
        modalMessage: document.getElementById('accessModalMessage'),
        modalCancel: document.getElementById('accessModalCancel'),
        modalSubmit: document.getElementById('accessModalSubmit'),
        changeForm: document.getElementById('accessChangeForm'),
        auditMessage: document.getElementById('accessAuditMessage'),
        auditList: document.getElementById('accessAuditList'),
        auditPrev: document.getElementById('accessAuditPrev'),
        auditNext: document.getElementById('accessAuditNext'),
        auditPageInfo: document.getElementById('accessAuditPageInfo'),
        confirm: document.getElementById('accessConfirm'),
        confirmBody: document.getElementById('accessConfirmBody'),
        confirmCancel: document.getElementById('accessConfirmCancel'),
        confirmOk: document.getElementById('accessConfirmOk')
    };

    var grid = new tui.Grid({
        el: document.getElementById('accessGrid'),
        data: [],
        width: 'auto',
        bodyHeight: 420,
        rowHeight: 48,
        scrollX: true,
        scrollY: true,
        header: {height: 48},
        columns: [
            {header: '번호', name: 'id', width: 70, align: 'center'},
            {header: '이름', name: 'name', minWidth: 110, align: 'center'},
            {header: '이메일', name: 'email', minWidth: 200, align: 'center'},
            {header: '휴대폰 번호', name: 'maskedPhoneNumber', minWidth: 140, align: 'center'},
            {header: '역할', name: 'roleLabel', width: 110, align: 'center'},
            {header: '상태', name: 'statusLabel', width: 90, align: 'center'},
            {header: '변경 일시', name: 'roleUpdatedAtLabel', minWidth: 170, align: 'center'},
            {
                header: '관리', name: 'action', width: 90, align: 'center',
                formatter: function () {
                    return '<button type="button" class="access-row-button">변경</button>';
                }
            }
        ]
    });

    var rowsById = {};

    grid.on('click', function (event) {
        if (!event.nativeEvent || !event.nativeEvent.target) {
            return;
        }
        if (!event.nativeEvent.target.classList.contains('access-row-button')) {
            return;
        }
        var row = grid.getRow(event.rowKey);
        if (row) {
            openModal(rowsById[row.id]);
        }
    });

    function showStatus(el, message, isError) {
        if (!message) {
            el.hidden = true;
            el.textContent = '';
            el.classList.remove('is-error');
            return;
        }
        el.hidden = false;
        el.textContent = message;
        el.classList.toggle('is-error', !!isError);
    }

    function formatDateTime(value) {
        return value ? String(value).replace('T', ' ').slice(0, 19) : '-';
    }

    function loadMembers() {
        showStatus(els.statusMessage, '불러오는 중입니다...', false);
        var params = new URLSearchParams();
        params.set('page', state.page);
        params.set('size', PAGE_SIZE);
        if (state.keyword) params.set('keyword', state.keyword);
        if (state.role) params.set('role', state.role);
        if (state.status) params.set('status', state.status);

        fetch(API_BASE + '?' + params.toString(), {
            headers: {'Accept': 'application/json'},
            credentials: 'same-origin'
        }).then(function (response) {
            if (response.status === 401) {
                window.location.href = '/login';
                return null;
            }
            if (response.status === 403) {
                throw new Error('접근 권한이 없습니다.');
            }
            if (!response.ok) {
                throw new Error('목록을 불러오지 못했습니다.');
            }
            return response.json();
        }).then(function (page) {
            if (!page) {
                return;
            }
            renderPage(page);
        }).catch(function (error) {
            grid.resetData([]);
            rowsById = {};
            els.count.textContent = '총 0명';
            showStatus(els.statusMessage, error.message || '네트워크 오류가 발생했습니다.', true);
            updatePager(0, 1);
        });
    }

    function renderPage(page) {
        rowsById = {};
        var rows = (page.content || []).map(function (item) {
            rowsById[item.id] = item;
            return {
                id: item.id,
                name: item.name,
                email: item.email,
                maskedPhoneNumber: item.maskedPhoneNumber,
                roleLabel: ROLE_LABEL[item.role] || item.role,
                statusLabel: STATUS_LABEL[item.status] || item.status,
                roleUpdatedAtLabel: formatDateTime(item.roleUpdatedAt)
            };
        });
        grid.resetData(rows);
        els.count.textContent = '총 ' + page.totalElements + '명';
        state.totalPages = Math.max(page.totalPages, 1);
        state.page = page.page;
        updatePager(page.page, state.totalPages);

        if (rows.length === 0) {
            showStatus(els.statusMessage, '조회된 회원이 없습니다.', false);
        } else {
            showStatus(els.statusMessage, '', false);
        }
    }

    function updatePager(page, totalPages) {
        els.pageInfo.textContent = (page + 1) + ' / ' + totalPages;
        els.prev.disabled = page <= 0;
        els.next.disabled = page >= totalPages - 1;
    }

    els.form.addEventListener('submit', function (event) {
        event.preventDefault();
        state.keyword = els.keyword.value.trim();
        state.role = els.role.value;
        state.status = els.status.value;
        state.page = 0;
        loadMembers();
    });

    els.prev.addEventListener('click', function () {
        if (state.page > 0) {
            state.page -= 1;
            loadMembers();
        }
    });

    els.next.addEventListener('click', function () {
        if (state.page < state.totalPages - 1) {
            state.page += 1;
            loadMembers();
        }
    });

    // ----- 모달 -----
    var lastFocused = null;

    function openModal(member) {
        if (!member) {
            return;
        }
        lastFocused = document.activeElement;
        els.modalName.textContent = member.name;
        els.modalEmail.textContent = member.email;
        els.modalMemberId.value = member.id;
        els.modalVersion.value = member.version;
        els.modalRole.value = member.role;
        els.modalStatus.value = member.status;
        els.modalReason.value = '';
        showStatus(els.modalMessage, '', false);
        els.modal.hidden = false;
        els.modalRole.focus();
        loadAudit(member.id, 0);
    }

    function closeModal() {
        els.modal.hidden = true;
        if (lastFocused && typeof lastFocused.focus === 'function') {
            lastFocused.focus();
        }
    }

    els.modalCancel.addEventListener('click', closeModal);
    els.modal.querySelector('[data-close]').addEventListener('click', closeModal);

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        if (!els.confirm.hidden) {
            hideConfirm();
        } else if (!els.modal.hidden) {
            closeModal();
        }
    });

    els.changeForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var reason = els.modalReason.value.trim();
        if (reason.length < 4) {
            showStatus(els.modalMessage, '변경 사유는 4자 이상 입력해 주세요.', true);
            els.modalReason.focus();
            return;
        }
        showConfirm();
    });

    // ----- 확인 다이얼로그 -----
    function showConfirm() {
        var roleLabel = ROLE_LABEL[els.modalRole.value] || els.modalRole.value;
        var statusLabel = STATUS_LABEL[els.modalStatus.value] || els.modalStatus.value;
        els.confirmBody.textContent =
            els.modalName.textContent + ' 회원을 역할 ' + roleLabel + ', 상태 ' + statusLabel + '(으)로 변경합니다.';
        els.confirm.hidden = false;
        els.confirmOk.focus();
    }

    function hideConfirm() {
        els.confirm.hidden = true;
        els.modalReason.focus();
    }

    els.confirmCancel.addEventListener('click', hideConfirm);

    els.confirmOk.addEventListener('click', function () {
        submitChange();
    });

    function submitChange() {
        var memberId = els.modalMemberId.value;
        var body = {
            role: els.modalRole.value,
            status: els.modalStatus.value,
            reason: els.modalReason.value.trim(),
            expectedVersion: Number(els.modalVersion.value)
        };
        els.confirmOk.disabled = true;
        els.modalSubmit.disabled = true;

        fetch(API_BASE + '/' + encodeURIComponent(memberId) + '/access', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            },
            credentials: 'same-origin',
            body: JSON.stringify(body)
        }).then(function (response) {
            if (response.status === 401) {
                window.location.href = '/login';
                return null;
            }
            return response.json().catch(function () {
                return {};
            }).then(function (data) {
                return {status: response.status, ok: response.ok, data: data};
            });
        }).then(function (result) {
            if (!result) {
                return;
            }
            hideConfirm();
            if (result.ok) {
                els.modalVersion.value = result.data.version;
                showStatus(els.modalMessage, '변경이 저장되었습니다.', false);
                loadMembers();
                loadAudit(Number(memberId), 0);
                return;
            }
            handleChangeError(result, Number(memberId));
        }).catch(function () {
            hideConfirm();
            showStatus(els.modalMessage, '네트워크 오류가 발생했습니다. 다시 시도해 주세요.', true);
        }).then(function () {
            els.confirmOk.disabled = false;
            els.modalSubmit.disabled = false;
        });
    }

    function handleChangeError(result, memberId) {
        var message = (result.data && result.data.message) || '변경에 실패했습니다.';
        showStatus(els.modalMessage, message, true);
        if (result.status === 409) {
            // 낙관적 잠금 충돌: 최신 데이터로 다시 조회하도록 안내한다.
            showStatus(els.modalMessage,
                message + ' 목록을 최신 데이터로 다시 불러왔습니다.', true);
            loadMembers();
        }
        if (result.status === 404) {
            loadMembers();
        }
    }

    // ----- 감사 로그 -----
    function loadAudit(memberId, page) {
        audit.memberId = memberId;
        audit.page = page;
        showStatus(els.auditMessage, '이력을 불러오는 중입니다...', false);
        els.auditList.innerHTML = '';

        var params = new URLSearchParams();
        params.set('page', page);
        params.set('size', AUDIT_PAGE_SIZE);

        fetch(API_BASE + '/' + encodeURIComponent(memberId) + '/audit-logs?' + params.toString(), {
            headers: {'Accept': 'application/json'},
            credentials: 'same-origin'
        }).then(function (response) {
            if (response.status === 401) {
                window.location.href = '/login';
                return null;
            }
            if (!response.ok) {
                throw new Error('이력을 불러오지 못했습니다.');
            }
            return response.json();
        }).then(function (data) {
            if (!data) {
                return;
            }
            renderAudit(data);
        }).catch(function (error) {
            showStatus(els.auditMessage, error.message || '이력을 불러오지 못했습니다.', true);
            updateAuditPager(0, 1);
        });
    }

    function renderAudit(page) {
        var items = page.content || [];
        if (items.length === 0) {
            showStatus(els.auditMessage, '변경 이력이 없습니다.', false);
            els.auditList.innerHTML = '';
            updateAuditPager(0, 1);
            return;
        }
        showStatus(els.auditMessage, '', false);
        els.auditList.innerHTML = '';
        items.forEach(function (entry) {
            var li = document.createElement('li');
            li.className = 'access-audit-item';

            var when = document.createElement('span');
            when.className = 'access-audit-when';
            when.textContent = formatDateTime(entry.createdAt);

            var change = document.createElement('span');
            change.className = 'access-audit-change';
            change.textContent =
                (ROLE_LABEL[entry.beforeRole] || entry.beforeRole) + '/' +
                (STATUS_LABEL[entry.beforeStatus] || entry.beforeStatus) + ' → ' +
                (ROLE_LABEL[entry.afterRole] || entry.afterRole) + '/' +
                (STATUS_LABEL[entry.afterStatus] || entry.afterStatus);

            var meta = document.createElement('span');
            meta.className = 'access-audit-meta';
            meta.textContent = '수행자 ' + (entry.actorName || ('#' + entry.actorMemberId)) +
                ' · IP ' + (entry.maskedRequestIp || '-');

            var reason = document.createElement('span');
            reason.className = 'access-audit-reason';
            reason.textContent = '사유: ' + entry.reason;

            li.appendChild(when);
            li.appendChild(change);
            li.appendChild(meta);
            li.appendChild(reason);
            els.auditList.appendChild(li);
        });

        audit.totalPages = Math.max(page.totalPages, 1);
        audit.page = page.page;
        updateAuditPager(page.page, audit.totalPages);
    }

    function updateAuditPager(page, totalPages) {
        els.auditPageInfo.textContent = (page + 1) + ' / ' + totalPages;
        els.auditPrev.disabled = page <= 0;
        els.auditNext.disabled = page >= totalPages - 1;
    }

    els.auditPrev.addEventListener('click', function () {
        if (audit.page > 0) {
            loadAudit(audit.memberId, audit.page - 1);
        }
    });

    els.auditNext.addEventListener('click', function () {
        if (audit.page < audit.totalPages - 1) {
            loadAudit(audit.memberId, audit.page + 1);
        }
    });

    window.addEventListener('resize', function () {
        grid.refreshLayout();
    });

    loadMembers();
}());
