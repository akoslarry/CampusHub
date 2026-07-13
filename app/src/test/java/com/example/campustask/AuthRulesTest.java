package com.example.campustask;

import com.example.campustask.model.AuthRules;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthRulesTest {
    @Test
    public void usernameRequiresAtLeastThreeVisibleCharacters() {
        assertFalse(AuthRules.isValidUsername("ab"));
        assertFalse(AuthRules.isValidUsername("   "));
        assertTrue(AuthRules.isValidUsername("student01"));
    }

    @Test
    public void passwordRequiresAtLeastSixCharacters() {
        assertFalse(AuthRules.isValidPassword("12345"));
        assertTrue(AuthRules.isValidPassword("123456"));
    }

    @Test
    public void registerPasswordMustMatchConfirmation() {
        assertFalse(AuthRules.passwordsMatch("123456", "123457"));
        assertTrue(AuthRules.passwordsMatch("123456", "123456"));
    }
}
