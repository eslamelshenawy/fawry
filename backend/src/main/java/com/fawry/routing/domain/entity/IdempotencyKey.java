package com.fawry.routing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class IdempotencyKey implements Serializable {

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(nullable = false, length = 64)
    private String scope;
}
