package com.example.campustask;

import com.example.campustask.model.AccountDatabaseRules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AccountDatabaseRulesTest {
    @Test
    public void createsStableDatabaseNameForSameAccount() {
        assertEquals(
                AccountDatabaseRules.businessDatabaseName("student01"),
                AccountDatabaseRules.businessDatabaseName(" student01 ")
        );
    }

    @Test
    public void differentAccountsUseDifferentDatabaseNames() {
        assertNotEquals(
                AccountDatabaseRules.businessDatabaseName("student01"),
                AccountDatabaseRules.businessDatabaseName("student02")
        );
    }

    @Test
    public void databaseNameUsesSafeCharactersOnly() {
        String name = AccountDatabaseRules.businessDatabaseName("张三@BISTU");

        assertTrue(name.startsWith("campus_user_"));
        assertTrue(name.endsWith(".db"));
        assertTrue(name.matches("[A-Za-z0-9_.]+"));
    }
}
