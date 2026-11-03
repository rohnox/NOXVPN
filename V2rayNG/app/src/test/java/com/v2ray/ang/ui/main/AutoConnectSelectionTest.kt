package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AutoConnectSelectionTest {
    @Test fun `chooses lowest positive fresh real delay not candidate ordering`() {
        val selection = AutoConnectSelection()
        selection.begin("run", listOf("slow", "fast", "dead", "untested"))
        selection.record("run", "slow", 200)
        selection.record("run", "fast", 30)
        selection.record("run", "dead", -1)
        assertEquals("fast", selection.finish("run", setOf("slow", "fast", "dead", "untested"))?.guid)
    }
    @Test fun `zero negative missing and deleted configurations never win`() {
        val selection = AutoConnectSelection()
        selection.begin("run", listOf("zero", "failed", "removed", "valid"))
        selection.record("run", "zero", 0)
        selection.record("run", "failed", -1)
        selection.record("run", "removed", 1)
        selection.record("run", "valid", 70)
        assertEquals("valid", selection.finish("run", setOf("zero", "failed", "valid"))?.guid)
    }
    @Test fun `empty and wholly failed runs do not make a choice`() {
        val selection = AutoConnectSelection()
        selection.begin("empty", emptyList())
        assertNull(selection.finish("empty", emptySet()))
        selection.begin("failed", listOf("a"))
        selection.record("failed", "a", -1)
        assertNull(selection.finish("failed", setOf("a")))
    }
    @Test fun `ties are stable and duplicate or foreign replies cannot replace results`() {
        val selection = AutoConnectSelection()
        selection.begin("run", listOf("a", "b", "a"))
        assertFalse(selection.record("other", "a", 1))
        assertFalse(selection.record("run", "foreign", 1))
        assertTrue(selection.record("run", "a", 50))
        assertFalse(selection.record("run", "a", 1))
        selection.record("run", "b", 50)
        assertEquals(2, selection.completedCount)
        assertEquals("a", selection.finish("run", setOf("a", "b"))?.guid)
    }
    @Test fun `cancel supersede late finish and duplicate consumption cannot activate`() {
        val selection = AutoConnectSelection()
        selection.begin("old", listOf("a"))
        selection.record("old", "a", 10)
        selection.cancel()
        assertNull(selection.finish("old", setOf("a")))
        selection.begin("new", listOf("b"))
        assertFalse(selection.record("old", "a", 1))
        assertNull(selection.finish("old", setOf("a")))
        selection.record("new", "b", 30)
        selection.finish("new", setOf("b"))
        assertNull(selection.consume("old"))
        assertNotNull(selection.consume("new"))
        assertNull(selection.consume("new"))
    }
    @Test fun `deadline may choose only already completed valid probes`() {
        val selection = AutoConnectSelection()
        selection.begin("run", listOf("responded", "pending"))
        selection.record("run", "responded", 90)
        assertEquals("responded", selection.finish("run", setOf("responded", "pending"))?.guid)
        assertFalse(selection.record("run", "pending", 1))
    }
}
