package com.nqd.nqd_lms_be.controller.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.nqd.nqd_lms_be.config.onehundredms.OneHundredMsProperties;
import com.nqd.nqd_lms_be.service.onehundredms.OneHundredMsWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/100ms")
@RequiredArgsConstructor
@Tag(name = "100ms Webhook", description = "Public webhook listener for 100ms live sessions and peer events")
public class OneHundredMsWebhookController {

    private final OneHundredMsWebhookService webhookService;
    private final OneHundredMsProperties properties;

    @GetMapping
    @Operation(summary = "Health check for 100ms Webhook endpoint")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "100ms webhook endpoint is ready and listening."
        ));
    }

    @PostMapping
    @Operation(summary = "Receive and process events from 100ms")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody(required = false) JsonNode payload,
            @RequestHeader HttpHeaders headers
    ) {
        // Optional secret verification
        if (properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank()) {
            String incomingSecret = headers.getFirst("X-100ms-Secret");
            if (incomingSecret == null) {
                incomingSecret = headers.getFirst("Authorization");
            }
            if (incomingSecret == null || !incomingSecret.trim().equals(properties.getWebhookSecret().trim())) {
                log.warn("Rejected 100ms webhook: missing or invalid secret header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", "error",
                        "message", "Unauthorized webhook request"
                ));
            }
        }

        try {
            if (payload != null && !payload.isNull()) {
                webhookService.handleWebhook(payload);
            }
        } catch (Exception e) {
            log.error("Error processing 100ms webhook event: {}", e.getMessage(), e);
            // Even if an internal exception occurs, return 200 so 100ms does not loop retry
        }

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Webhook processed successfully"
        ));
    }
}
