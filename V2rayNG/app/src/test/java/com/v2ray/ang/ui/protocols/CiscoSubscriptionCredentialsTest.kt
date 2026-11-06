package com.v2ray.ang.ui.protocols

import app.openconnect.AuthFormHandler
import org.infradead.libopenconnect.LibOpenConnect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CiscoSubscriptionCredentialsTest {
    @Test fun prefillsOnlyAccountFieldsAndDoesNotOverwriteServerOrOtpValues() {
        val text = LibOpenConnect.OC_FORM_OPT_TEXT
        val password = LibOpenConnect.OC_FORM_OPT_PASSWORD
        assertEquals("fixture-user", AuthFormHandler.subscriptionFieldDefault(text, "username", "", "fixture-user", "fixture-pass"))
        assertEquals("fixture-pass", AuthFormHandler.subscriptionFieldDefault(password, "password", null, "fixture-user", "fixture-pass"))
        assertEquals("server-value", AuthFormHandler.subscriptionFieldDefault(password, "password", "server-value", "fixture-user", "fixture-pass"))
        for (name in listOf("otp", "passcode", "token", "second_password")) assertNull(AuthFormHandler.subscriptionFieldDefault(password, name, null, "fixture-user", "fixture-pass"))
        assertNull(AuthFormHandler.subscriptionFieldDefault(text, "password", null, "fixture-user", "fixture-pass"))
    }
}
