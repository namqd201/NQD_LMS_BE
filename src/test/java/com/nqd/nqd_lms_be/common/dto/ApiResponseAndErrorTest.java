package com.nqd.nqd_lms_be.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseAndErrorTest {

    @Test
    @DisplayName("Should create ApiResponse with data and success true")
    void testApiResponseSuccess() {
        ApiResponse<String> response = ApiResponse.ok("Thao tác thành công", "Dữ liệu trả về");
        assertTrue(response.isSuccess());
        assertEquals("Thao tác thành công", response.getMessage());
        assertEquals("Dữ liệu trả về", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("Should create PageResponse from Spring Page")
    void testPageResponse() {
        List<String> items = List.of("Item 1", "Item 2", "Item 3");
        PageImpl<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 3);

        PageResponse<String> pageResponse = PageResponse.fromPage(page);
        assertEquals(3, pageResponse.getItems().size());
        assertEquals(0, pageResponse.getPageNumber());
        assertEquals(10, pageResponse.getPageSize());
        assertEquals(3L, pageResponse.getTotalElements());
        assertEquals(1, pageResponse.getTotalPages());
        assertTrue(pageResponse.isFirst());
        assertTrue(pageResponse.isLast());
        assertFalse(pageResponse.isHasNext());
    }

    @Test
    @DisplayName("Should build ErrorResponse with status and errorCode")
    void testErrorResponse() {
        ErrorResponse err = ErrorResponse.of("Không tìm thấy sản phẩm", "PRODUCT_NOT_FOUND", 404);
        assertFalse(err.isSuccess());
        assertEquals(404, err.getStatus());
        assertEquals("PRODUCT_NOT_FOUND", err.getErrorCode());
        assertEquals("Không tìm thấy sản phẩm", err.getMessage());
    }
}
