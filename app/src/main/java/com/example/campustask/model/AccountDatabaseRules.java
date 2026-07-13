package com.example.campustask.model;

public final class AccountDatabaseRules {
    private AccountDatabaseRules() {
    }

    public static String businessDatabaseName(String username) {
        String normalized = username == null ? "guest" : username.trim();
        if (normalized.isEmpty()) {
            normalized = "guest";
        }
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                safe.append(c);
            } else {
                safe.append('_');
            }
        }
        String safeName = safe.toString().replaceAll("_+", "_");
        if (safeName.startsWith("_")) {
            safeName = safeName.substring(1);
        }
        if (safeName.endsWith("_")) {
            safeName = safeName.substring(0, safeName.length() - 1);
        }
        if (safeName.isEmpty()) {
            safeName = "account";
        }
        return "campus_user_" + safeName + "_" + Integer.toHexString(normalized.hashCode()) + ".db";
    }
}
