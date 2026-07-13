package com.example.campustask.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClassroomSearchRules {
    private ClassroomSearchRules() {
    }

    public static List<ClassroomSlot> filter(List<ClassroomSlot> slots, String query) {
        List<ClassroomSlot> result = new ArrayList<>();
        if (slots == null) {
            return result;
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            result.addAll(slots);
            return result;
        }
        for (ClassroomSlot slot : slots) {
            if (slot != null && matches(slot, normalizedQuery)) {
                result.add(slot);
            }
        }
        return result;
    }

    private static boolean matches(ClassroomSlot slot, String normalizedQuery) {
        return normalize(slot.room).contains(normalizedQuery)
                || normalize(slot.capacity).contains(normalizedQuery)
                || normalize(slot.time).contains(normalizedQuery);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.CHINA);
    }
}
