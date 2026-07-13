package com.example.campustask;

import com.example.campustask.model.ReservationRules;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReservationRulesTest {
    @Test
    public void removesReservationByIndex() {
        List<String> reservations = new ArrayList<>(Arrays.asList(
                "A101 智慧教室 · 08:00-10:00",
                "B204 研讨室 · 14:00-16:00"
        ));

        boolean removed = ReservationRules.removeAt(reservations, 0);

        assertTrue(removed);
        assertEquals(1, reservations.size());
        assertEquals("B204 研讨室 · 14:00-16:00", reservations.get(0));
    }

    @Test
    public void rejectsInvalidReservationIndex() {
        List<String> reservations = new ArrayList<>(Arrays.asList("A101 智慧教室 · 08:00-10:00"));

        boolean removed = ReservationRules.removeAt(reservations, 3);

        assertFalse(removed);
        assertEquals(1, reservations.size());
    }
}
