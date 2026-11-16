package com.example.data

import android.util.Log
import com.example.data.abstraction.LiveCaptionTranslator
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class TranslationLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String
) {
    ORIGINAL("en", "Original (English)", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    ARABIC("ar", "Arabic", "العربية"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    GERMAN("de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "日本語"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    BENGALI("bn", "Bengali", "বাংলা")
}

class FirebaseLiveTranslator : LiveCaptionTranslator {

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "translations" to Schema.array(
                items = Schema.string(),
                description = "Translated strings in the exact same order as the input list."
            )
        )
    )

    private val translationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = translationSchema
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = translationConfig
        )
    }

    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = translationConfig
        )
    }

    private val cache = ConcurrentHashMap<String, String>()

    override suspend fun translateBatch(
        lines: List<String>,
        targetLanguage: TranslationLanguage
    ): Result<List<String>> {
        if (targetLanguage == TranslationLanguage.ORIGINAL || lines.isEmpty()) {
            return Result.success(lines)
        }

        val distinctMissing = lines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !cache.containsKey(cacheKey(targetLanguage.code, it)) }
            .distinct()

        if (distinctMissing.isNotEmpty()) {
            val prompt = buildString {
                appendLine("Translate the following live sports commentary and scoreboard lines into ${targetLanguage.displayName} (${targetLanguage.code}).")
                appendLine("Keep team abbreviations, numbers, scores, and clock values intact. Keep translations concise and natural for live TV captions.")
                appendLine("Input lines (${distinctMissing.size} items):")
                distinctMissing.forEachIndexed { idx, line ->
                    appendLine("${idx + 1}. $line")
                }
            }

            val models = listOf(primaryModel, fallbackModel, primaryModel)
            var lastError: Exception? = null
            var succeeded = false

            for ((attempt, model) in models.withIndex()) {
                try {
                    if (attempt > 0) delay(700L)
                    val response = withTimeout(30_000L) {
                        model.generateContent(prompt)
                    }
                    val jsonText = response.text ?: error("Translation service returned an empty response.")
                    val json = JSONObject(jsonText)
                    val arr = json.getJSONArray("translations")
                    for (i in 0 until minOf(distinctMissing.size, arr.length())) {
                        val translated = arr.optString(i).trim()
                        if (translated.isNotEmpty()) {
                            cache[cacheKey(targetLanguage.code, distinctMissing[i])] = translated
                        }
                    }
                    succeeded = true
                    break
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Translation timed out on attempt ${attempt + 1}")
                    lastError = Exception("Translation timed out. Please try again.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ServerException) {
                    Log.w(TAG, "Transient translation server exception on attempt ${attempt + 1}")
                    lastError = e
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed on attempt ${attempt + 1}")
                    lastError = e
                    break
                }
            }

            if (!succeeded) {
                return Result.failure(
                    Exception(lastError?.localizedMessage ?: "Live translation temporarily unavailable.")
                )
            }
        }

        val resolved = lines.map { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) raw
            else cache[cacheKey(targetLanguage.code, trimmed)] ?: raw
        }
        return Result.success(resolved)
    }

    private fun cacheKey(langCode: String, text: String): String = "$langCode::$text"

    private companion object {
        const val TAG = "FirebaseLiveTranslator"
    }
}
