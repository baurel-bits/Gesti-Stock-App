package com.stock.api.entity;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("Administrateur"),
    MANAGER("Gestionnaire"),
    USER("Utilisateur");

    private final String description;

    Role(String description) {
        this.description = description;
    }
}
