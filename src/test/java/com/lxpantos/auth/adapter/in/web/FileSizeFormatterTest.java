package com.lxpantos.auth.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSizeFormatterTest {
    private final FileSizeFormatter formatter = new FileSizeFormatter();

    @ParameterizedTest
    @CsvSource({
            // 바이트: 정수, ' B' 표기
            "0, 0 B",
            "1, 1 B",
            "512, 512 B",
            "1023, 1023 B",
            // 정확히 1024 경계 → KB, 소수점 1자리
            "1024, 1.0 KB",
            "1536, 1.5 KB",           // 1.5 KB 정확 값
            "2048, 2.0 KB",
            // KB/MB/GB 임계값
            "1048575, 1024.0 KB",     // 1024^2 - 1, MB 미만
            "1048576, 1.0 MB",        // 1024^2
            "1073741823, 1024.0 MB",  // 1024^3 - 1, GB 미만
            "1073741824, 1.0 GB",     // 1024^3
            // 반올림(HALF_UP, 소수점 1자리)
            "1126, 1.1 KB",           // 1126/1024 = 1.0996 → 1.1
            "1075, 1.0 KB",           // 1075/1024 = 1.0498 → 1.0
            "1587, 1.5 KB",           // 1587/1024 = 1.5498 → 1.5
            "1613, 1.6 KB",           // 1613/1024 = 1.5752 → 1.6
            // 10 MiB 제한 부근
            "10485760, 10.0 MB",      // 정확히 10 MiB
            "10380902, 9.9 MB"        // 10 MiB 바로 아래
    })
    void formatsBytesAsHumanReadableBinaryUnits(long bytes, String expected) {
        assertThat(formatter.format(bytes)).isEqualTo(expected);
    }

    @Test
    void rejectsNegativeSize() {
        assertThatThrownBy(() -> formatter.format(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
