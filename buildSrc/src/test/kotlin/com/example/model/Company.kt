package com.example.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Any
import kotlin.Array
import kotlin.Long
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

public data class Company(
    public val name: String? = null,
    public val ratings: Ratings? = null,
    public val information: Information? = null,
    public val additionalProperties: Map<String, Any?> = emptyMap(),
) {
    public fun toJson(): JsonElement {
        val json = JsonObject()
        name?.let { json.addProperty("name", it) }
        ratings?.let { json.add("ratings", it.toJson()) }
        information?.let { json.add("information", it.toJson()) }
        additionalProperties.forEach { (k, v) ->
            json.add(k, v.toJsonElement())
        }
        return json
    }

    public companion object {
        internal val RESERVED_PROPERTIES: Array<String> = arrayOf("name", "ratings", "information")

        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): Company {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val name = jsonObject.get("name")?.asString
                val ratings = jsonObject.get("ratings")?.toString()?.let {
                    Ratings.fromJson(it)
                }
                val information = jsonObject.get("information")?.toString()?.let {
                    Information.fromJson(it)
                }
                val additionalProperties = mutableMapOf<String, Any?>()
                for (entry in jsonObject.entrySet()) {
                    if (entry.key !in RESERVED_PROPERTIES) {
                        additionalProperties[entry.key] = entry.value
                    }
                }
                return Company(name, ratings, information, additionalProperties)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }

    public data class Ratings(
        public val global: Long,
        public val additionalProperties: Map<String, Long> = emptyMap(),
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("global", global)
            additionalProperties.forEach { (k, v) ->
                json.addProperty(k, v)
            }
            return json
        }

        public companion object {
            internal val RESERVED_PROPERTIES: Array<String> = arrayOf("global")

            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Ratings {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val global = jsonObject.get("global").asLong
                    val additionalProperties = mutableMapOf<String, Long>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key !in RESERVED_PROPERTIES) {
                            additionalProperties[entry.key] = entry.value.asLong
                        }
                    }
                    return Ratings(global, additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    public data class Information(
        public val date: Long? = null,
        public val priority: Long? = null,
        public val additionalProperties: Map<String, Map<String, Any?>> = emptyMap(),
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            date?.let { json.addProperty("date", it) }
            priority?.let { json.addProperty("priority", it) }
            additionalProperties.forEach { (k, v) ->
                json.add(k, v.toJsonElement())
            }
            return json
        }

        public companion object {
            internal val RESERVED_PROPERTIES: Array<String> = arrayOf("date", "priority")

            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Information {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val date = jsonObject.get("date")?.asLong
                    val priority = jsonObject.get("priority")?.asLong
                    val additionalProperties = mutableMapOf<String, Map<String, Any?>>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key !in RESERVED_PROPERTIES) {
                            additionalProperties[entry.key] = entry.value.asMap()
                        }
                    }
                    return Information(date, priority, additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }
}
