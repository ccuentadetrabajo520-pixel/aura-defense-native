package com.aura.defense.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityKnowledgeBaseTest {
    @Test
    fun `la base contiene cobertura amplia`() {
        assertTrue(SecurityKnowledgeBase.entries.size >= 130)
    }

    @Test
    fun `busca phishing y sim swapping`() {
        assertNotNull(SecurityKnowledgeBase.search("que es phishing"))
        assertNotNull(SecurityKnowledgeBase.search("sim swapping"))
    }

    @Test
    fun `normaliza acentos`() {
        assertEquals("fishing ae", SecurityKnowledgeBase.normalize("fishing áé"))
    }
}
