package com.example.campustask.model;

/**
 * 教室可预约时间段。
 * 每个教室有多个时间段，每个时间段可被分割为1小时粒度的预约。
 */
public class ClassroomSlot {
    public final String room;
    public final String capacity;
    public final String time;
    public final int startHour;
    public final int endHour;

    public ClassroomSlot(String room, String capacity, String time) {
        this(room, capacity, time, 0, 0);
    }

    public ClassroomSlot(String room, String capacity, String time, int startHour, int endHour) {
        this.room = room;
        this.capacity = capacity;
        this.time = time;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    /**
     * 生成该时间段内所有1小时子时段的列表。
     * 例如 08:00-10:00 → ["08:00-09:00", "09:00-10:00"]
     */
    public java.util.List<String> subSlots() {
        java.util.List<String> slots = new java.util.ArrayList<>();
        for (int h = startHour; h < endHour; h++) {
            slots.add(String.format("%02d:00-%02d:00", h, h + 1));
        }
        return slots;
    }

    /**
     * 检查某个子时段是否属于此教室的可用时间范围。
     */
    public boolean containsSubSlot(int fromHour, int toHour) {
        return fromHour >= startHour && toHour <= endHour && toHour > fromHour;
    }
}
