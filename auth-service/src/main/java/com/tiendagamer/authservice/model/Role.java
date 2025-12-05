package com.tiendagamer.authservice.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ADMIN,
    END_USER;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}