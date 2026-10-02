package com.saaf.milkshop.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the hand-written structural comparer used by C1 ack
 * equality — this must behave identically on the JVM and on device, which
 * is exactly why JSONObject.similar() was rejected.
 */
class JsonStructuralTest {

    @Test
    fun equalPayloads_match_regardlessOfKeyOrderAndNumberTypes() {
        val a = JSONObject().put("name", "Milk").put("quantity", 2).put("ratio", 0.5)
        val b = JSONObject().put("ratio", 0.5).put("quantity", 2L).put("name", "Milk")
        assertTrue(jsonStructurallyEqual(a, b))
    }

    @Test
    fun nestedObjectsAndArrays_compareStructurally() {
        val a = JSONObject()
            .put("profile", JSONObject().put("phone", "0300"))
            .put("logs", JSONArray().put(JSONObject().put("q", 1.5)))
        val b = JSONObject()
            .put("logs", JSONArray().put(JSONObject().put("q", 1.5)))
            .put("profile", JSONObject().put("phone", "0300"))
        assertTrue(jsonStructurallyEqual(a, b))

        b.put("extra", 1)
        assertFalse(jsonStructurallyEqual(a, b))
    }

    @Test
    fun nullAndJSONObjectNULL_areEqual_missingIsNot() {
        // JSON-java's put(key, null) REMOVES the key — JSON null is only
        // storable as JSONObject.NULL (or as an array element).
        val a = JSONObject().put("notes", JSONObject.NULL)
        val b = JSONObject().put("notes", JSONObject.NULL)
        assertTrue(jsonStructurallyEqual(a, b))

        val withoutField = JSONObject()
        assertFalse(jsonStructurallyEqual(a, withoutField))

        assertTrue(jsonStructurallyEqual(
            JSONArray().put(JSONObject.NULL),
            JSONArray().put(null),
        ))
    }

    @Test
    fun differentValues_doNotMatch() {
        assertFalse(jsonStructurallyEqual(
            JSONObject().put("v", 1.005),
            JSONObject().put("v", 1.006),
        ))
        assertFalse(jsonStructurallyEqual(
            JSONObject().put("v", "a"),
            JSONObject().put("v", "b"),
        ))
        val arrA = JSONArray().put(1).put(2)
        val arrB = JSONArray().put(2).put(1)
        assertFalse(jsonStructurallyEqual(arrA, arrB))
    }
}
