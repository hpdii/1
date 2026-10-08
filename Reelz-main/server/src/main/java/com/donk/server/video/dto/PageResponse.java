package com.donk.server.video.dto;

import java.util.List;

/**
 * Boc page trong mot object thay vi tra mang tran.
 *
 * Khong serialize thang Page<T> cua Spring Data: PageImpl co hinh dang JSON
 * khong on dinh va Boot 3 con ghi canh bao ve viec nay.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        boolean hasMore) {
}
