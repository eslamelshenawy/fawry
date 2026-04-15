package com.fawry.routing.service.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fawry.routing.domain.entity.IdempotencyKey;
import com.fawry.routing.domain.entity.IdempotencyRecord;
import com.fawry.routing.exception.IdempotencyConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordWriter writer;
    private final ObjectMapper objectMapper;

    public <T> T executeOnce(String rawKey, String scope, String principal,
                             Object request, Class<T> responseType, Supplier<T> action) {
        if (rawKey == null || rawKey.isBlank()) {
            return action.get();
        }

        IdempotencyKey key = new IdempotencyKey(sha256(rawKey), scope);
        String requestHash = sha256(serialize(request));

        return writer.findById(key)
                .map(existing -> replay(existing, requestHash, responseType))
                .orElseGet(() -> runAndStore(key, principal, requestHash, responseType, action));
    }

    private <T> T runAndStore(IdempotencyKey key, String principal, String requestHash,
                              Class<T> responseType, Supplier<T> action) {
        T response = action.get();
        String responseJson = serialize(response);
        return writer.saveIfAbsent(key, principal, requestHash, responseJson)
                .map(saved -> response)
                .orElseGet(() -> replayAfterRace(key, requestHash, responseType));
    }

    private <T> T replayAfterRace(IdempotencyKey key, String requestHash, Class<T> responseType) {
        return writer.findById(key)
                .map(existing -> replay(existing, requestHash, responseType))
                .orElseThrow(() -> new IdempotencyConflictException(
                        "Idempotency race condition on key " + key.getKeyHash().substring(0, 8)));
    }

    private <T> T replay(IdempotencyRecord record, String requestHash, Class<T> responseType) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key reused with a different request payload");
        }
        return deserialize(record.getResponseJson(), responseType);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize idempotency payload", ex);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize idempotency payload", ex);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
