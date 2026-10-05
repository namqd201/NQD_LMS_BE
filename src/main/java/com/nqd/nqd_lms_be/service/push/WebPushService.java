package com.nqd.nqd_lms_be.service.push;

import com.nqd.nqd_lms_be.dto.push.PushSubscriptionDto;
import com.nqd.nqd_lms_be.entity.User;

import java.util.List;
import java.util.UUID;

public interface WebPushService {

    String getVapidPublicKey();

    void subscribe(User user, PushSubscriptionDto request);

    void unsubscribe(String endpoint);

    void sendPushToUser(UUID userId, String title, String body, String linkUrl);

    void sendPushToUsers(List<UUID> userIds, String title, String body, String linkUrl);
}
