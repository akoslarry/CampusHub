package com.example.campustask;

import com.example.campustask.model.ClassroomSearchRules;
import com.example.campustask.model.ClassroomSlot;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClassroomSearchRulesTest {
    private final List<ClassroomSlot> slots = Arrays.asList(
            new ClassroomSlot("A101 智慧教室", "80 人", "08:00-10:00"),
            new ClassroomSlot("B204 研讨室", "24 人", "14:00-16:00"),
            new ClassroomSlot("图书馆 302", "12 人", "19:00-21:00")
    );

    @Test
    public void blankQueryReturnsAllClassrooms() {
        assertEquals(3, ClassroomSearchRules.filter(slots, "").size());
        assertEquals(3, ClassroomSearchRules.filter(slots, "   ").size());
    }

    @Test
    public void filtersByRoomNameOrLocation() {
        List<ClassroomSlot> result = ClassroomSearchRules.filter(slots, "图书馆");

        assertEquals(1, result.size());
        assertEquals("图书馆 302", result.get(0).room);
    }

    @Test
    public void filtersByCapacityOrTime() {
        assertEquals("A101 智慧教室", ClassroomSearchRules.filter(slots, "80").get(0).room);
        assertEquals("B204 研讨室", ClassroomSearchRules.filter(slots, "14:00").get(0).room);
    }
}
