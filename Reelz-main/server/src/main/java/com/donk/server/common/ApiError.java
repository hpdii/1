package com.donk.server.common;

/**
 * Hinh dang duy nhat cho MOI phan hoi loi.
 *
 * Bat buoc phai nhat quan: Gson ben client nem JsonSyntaxException ngay khi gap
 * trang HTML loi hoac body rong, va nguoi dung chi thay "loi ket noi" thay vi
 * thong bao that su.
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path) {
}
