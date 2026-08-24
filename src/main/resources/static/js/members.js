(function () {
    tui.Grid.applyTheme('default', {
        outline: {
            border: '#c5cad3',
            showVerticalBorder: true
        },
        cell: {
            normal: {
                border: '#d0d5dd',
                showVerticalBorder: true
            },
            header: {
                border: '#d0d5dd',
                showVerticalBorder: true,
                background: '#fafafa'
            }
        }
    });

    const count = document.getElementById('memberCount');
    const grid = new tui.Grid({
        el: document.getElementById('memberGrid'),
        data: [],
        bodyHeight: 400,
        rowHeight: 48,
        scrollX: false,
        header: {height: 48},
        columns: [
            {header: '번호', name: 'id', width: 90, align: 'center'},
            {header: '이름', name: 'name', minWidth: 160, align: 'center'},
            {header: '이메일', name: 'email', minWidth: 260, align: 'center'},
            {header: '가입일', name: 'createdAt', width: 140, align: 'center'}
        ]
    });

    function render(data) {
        grid.resetData(data);
        count.textContent = `총 ${data.length}명`;
    }

    function load(keyword) {
        const url = keyword
            ? `/api/members?keyword=${encodeURIComponent(keyword)}`
            : '/api/members';

        fetch(url, {
            headers: {'Accept': 'application/json'},
            credentials: 'same-origin'
        }).then(function (response) {
            if (response.status === 401) {
                window.location.href = '/login';
                return null;
            }
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return response.json();
        }).then(function (rows) {
            if (!rows) {
                return;
            }
            render(rows.map(function (row) {
                return {
                    id: row.id,
                    name: row.name,
                    email: row.email,
                    createdAt: row.createdAt ? row.createdAt.slice(0, 10) : ''
                };
            }));
        }).catch(function () {
            count.textContent = '목록을 불러오지 못했습니다.';
        });
    }

    document.getElementById('memberSearchForm').addEventListener('submit', function (event) {
        event.preventDefault();
        load(document.getElementById('memberKeyword').value.trim());
    });

    load('');
}());
