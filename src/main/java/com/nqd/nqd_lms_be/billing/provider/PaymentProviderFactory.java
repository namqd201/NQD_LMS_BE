package com.nqd.nqd_lms_be.billing.provider;

import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final List<PaymentProvider> paymentProviders;
    private final PaymentProperties paymentProperties;
    private final Map<String, PaymentProvider> providerCache = new ConcurrentHashMap<>();

    public PaymentProvider getProvider(String providerName) {
        String targetName = (providerName != null && !providerName.isBlank())
                ? providerName.toUpperCase().trim()
                : paymentProperties.getProvider().toUpperCase().trim();

        return providerCache.computeIfAbsent(targetName, name -> {
            Optional<PaymentProvider> matched = paymentProviders.stream()
                    .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                    .findFirst();

            if (matched.isPresent()) {
                return matched.get();
            }

            // Fallback to default configured provider or first available provider
            log.warn("Payment provider '{}' not found, falling back to default '{}'", name, paymentProperties.getProvider());
            return paymentProviders.stream()
                    .filter(p -> p.getProviderName().equalsIgnoreCase(paymentProperties.getProvider()))
                    .findFirst()
                    .orElseGet(() -> paymentProviders.isEmpty() ? null : paymentProviders.get(0));
        });
    }

    public PaymentProvider getDefaultProvider() {
        return getProvider(paymentProperties.getProvider());
    }
}
