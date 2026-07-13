package com.example.campustask.model;

import java.util.List;

public final class ReservationRules {
    private ReservationRules() {
    }

    public static boolean removeAt(List<String> reservations, int index) {
        if (reservations == null || index < 0 || index >= reservations.size()) {
            return false;
        }
        reservations.remove(index);
        return true;
    }

    /**
     * 检查新预约是否与已有预约时间冲突（同一时段不能预约两个教室）。
     * 预约记录格式："教室名 · HH:00-HH:00"
     */
    public static boolean hasTimeConflict(List<String> reservations, String newTimeSlot) {
        if (newTimeSlot == null) {
            return false;
        }
        String newTime = extractTime(newTimeSlot);
        if (newTime.isEmpty()) {
            return false;
        }
        for (String reservation : reservations) {
            String existingTime = extractTime(reservation);
            if (!existingTime.isEmpty() && existingTime.equals(newTime)) {
                return true;
            }
        }
        return false;
    }

    private static String extractTime(String reservation) {
        if (reservation == null) {
            return "";
        }
        int dotIndex = reservation.lastIndexOf("\u00b7");
        if (dotIndex >= 0 && dotIndex < reservation.length() - 1) {
            return reservation.substring(dotIndex + 1).trim();
        }
        return "";
    }

    /**
     * 检查某个教室的某一时段是否已被预约。
     */
    public static boolean isSlotBooked(List<String> reservations, String room, String timeSlot) {
        String booking = room + " \u00b7 " + timeSlot;
        for (String reservation : reservations) {
            if (reservation.equals(booking)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查教室的所有子时段是否都已被预约（占满）。
     */
    public static boolean isFullyBooked(List<String> reservations, String room, List<String> subSlots) {
        for (String slot : subSlots) {
            if (!isSlotBooked(reservations, room, slot)) {
                return false;
            }
        }
        return true;
    }
}
