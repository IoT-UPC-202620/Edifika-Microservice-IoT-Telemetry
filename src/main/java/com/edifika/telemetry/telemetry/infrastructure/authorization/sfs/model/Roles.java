package com.edifika.telemetry.telemetry.infrastructure.authorization.sfs.model;

public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String DEFAULT = "USER";

    private static final String AUTHORITY_PREFIX = "ROLE_";

    private Roles() {}

    public static String toAuthority(String role) {
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith(AUTHORITY_PREFIX) ? normalized : AUTHORITY_PREFIX + normalized;
    }
}
