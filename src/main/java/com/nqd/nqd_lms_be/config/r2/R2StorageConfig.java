package com.nqd.nqd_lms_be.config.r2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class R2StorageConfig {

    private final R2Properties r2Properties;

    @Bean
    public S3Client r2S3Client() {
        if (!r2Properties.isConfigured()) {
            log.info("Cloudflare R2 is not fully configured (enabled=false or missing credentials). Initializing empty/dummy client.");
            // Return a minimal client or fallback
            return S3Client.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("dummy-access-key", "dummy-secret-key")
                    ))
                    .build();
        }

        String endpoint = "https://" + r2Properties.getAccountId().trim() + ".r2.cloudflarestorage.com";
        log.info("Initializing Cloudflare R2 S3Client with endpoint: {}", endpoint);

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2Properties.getAccessKey().trim(), r2Properties.getSecretKey().trim())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}
