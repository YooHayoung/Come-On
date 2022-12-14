package com.comeon.userservice.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.stream.Stream;

public enum OAuthProvider {
    KAKAO("카카오"),
    ;

    private final String description;

    OAuthProvider(String description) {
        this.description = description;
    }

    @JsonCreator
    public static OAuthProvider from(String value) {
        return Stream.of(values())
                .filter(provider -> provider.name().equals(value.toUpperCase()))
                .findFirst()
                .orElse(null);
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public String getDescription() {
        return description;
    }
}

