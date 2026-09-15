package com.lxpantos.auth.adapter.in.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 첨부 크기를 사람이 읽기 쉬운 이진(1024 기반) 단위로 변환하는 표시 전용 포매터.
 *
 * <p>규모에 맞춰 B/KB/MB/GB/... 단위를 선택한다. 1024 바이트 미만은 정수 바이트({@code B}),
 * KB 이상은 소수점 1자리로 표기한다. 저장된 바이트 값이나 검증 로직에는 영향을 주지 않는
 * 화면 표기용 변환이며, 저장 키·파일 경로·다운로드 주소를 포함하지 않는다.
 *
 * <p>검증 대상: 요구사항 5.20 (IA-07), Design 속성 9.
 */
@Component
public class FileSizeFormatter {
    private static final long UNIT = 1024L;
    private static final String[] LARGE_UNITS = {"KB", "MB", "GB", "TB", "PB", "EB"};

    /**
     * 바이트 값을 이진 단위 문자열로 변환한다.
     *
     * @param bytes 저장된 파일 크기(바이트, 음수 아님)
     * @return 예: {@code "0 B"}, {@code "512 B"}, {@code "1.0 KB"}, {@code "10.0 MB"}
     */
    public String format(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("파일 크기는 음수일 수 없습니다.");
        }
        if (bytes < UNIT) {
            return bytes + " B";
        }
        double value = bytes;
        int unitIndex = -1;
        while (value >= UNIT && unitIndex < LARGE_UNITS.length - 1) {
            value /= UNIT;
            unitIndex++;
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
        return String.format(Locale.ROOT, "%s %s", rounded.toPlainString(), LARGE_UNITS[unitIndex]);
    }
}
