package com.saaf.milkshop.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-written structural equality for wire envelopes (C1 ack equality).
 * Deliberately NOT JSONObject.similar(): Android's org.json is not the
 * JSON-java library, and ack equality must behave identically on JVM tests
 * and on device. Rules: numbers compare numerically (Int/Long/Double),
 * JSONObject.NULL equals JSON null, key order is irrelevant (org.json is
 * HashMap-backed, so byte-comparing re-serializations would never match).
 */
internal fun jsonStructurallyEqual(a: JSONObject, b: JSONObject): Boolean {
    if (a.length() != b.length()) return false
    val keys = a.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (!b.has(key)) return false
        if (!jsonStructurallyEqual(a.opt(key), b.opt(key))) return false
    }
    return true
}

internal fun jsonStructurallyEqual(a: JSONArray, b: JSONArray): Boolean {
    if (a.length() != b.length()) return false
    for (index in 0 until a.length()) {
        if (!jsonStructurallyEqual(a.opt(index), b.opt(index))) return false
    }
    return true
}

private fun jsonStructurallyEqual(a: Any?, b: Any?): Boolean = when {
    a is JSONObject && b is JSONObject -> jsonStructurallyEqual(a, b)
    a is JSONArray && b is JSONArray -> jsonStructurallyEqual(a, b)
    a is Number && b is Number ->
        // Integral values must compare exactly: org.json yields Int/Long for
        // integer literals, and a double round-trip collapses distinct Longs
        // anywhere above 2^53 (review finding, go 7).
        if ((a is Int || a is Long) && (b is Int || b is Long)) a.toLong() == b.toLong()
        else a.toDouble() == b.toDouble()
    a == null || a === JSONObject.NULL -> b == null || b === JSONObject.NULL
    else -> a == b
}
