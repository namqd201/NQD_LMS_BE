package com.nqd.nqd_lms_be.common.exception;

import com.nqd.nqd_lms_be.dto.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.of("Bạn không có quyền truy cập hoặc thực hiện thao tác này."));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<MessageResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Bạn đang gửi yêu cầu quá nhanh. Vui lòng chờ trong giây lát rồi thử lại."));
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<MessageResponse> handleLimitExceededException(LimitExceededException ex) {
        log.warn("Usage limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Bạn đã đạt giới hạn sử dụng của gói dịch vụ hiện tại. Vui lòng nâng cấp lên gói VIP/Pro để tiếp tục."));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<MessageResponse> handleForbiddenOperationException(ForbiddenOperationException ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Bạn không có quyền thực hiện thao tác này đối với tài nguyên được chọn."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Không tìm thấy dữ liệu yêu cầu hoặc tài nguyên đã bị xóa."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse.of("Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại để tiếp tục sử dụng."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Dữ liệu yêu cầu không hợp lệ. Vui lòng kiểm tra lại."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại các trường thông tin.");
        response.put("errors", errors);
        response.put("success", false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<MessageResponse> handleOptimisticLockingFailureException(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse.of("Dữ liệu vừa được cập nhật bởi một phiên làm việc khác. Vui lòng tải lại trang và thử lại."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MessageResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.of(ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Trạng thái giao dịch không hợp lệ để thực hiện hành động này."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse.of("Đã xảy ra sự cố hệ thống không mong muốn. Vui lòng thử lại sau hoặc liên hệ quản trị viên."));
    }
}
