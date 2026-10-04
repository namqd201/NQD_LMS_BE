package com.nqd.nqd_lms_be.service.onehundredms;

import com.fasterxml.jackson.databind.JsonNode;

public interface OneHundredMsWebhookService {
    void handleWebhook(JsonNode payload);
}
