package com.aura.defense.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScoreEntry(
        val score: Int,
            val status: String,
                val timestamp: Long
) {
        val formattedTime: String get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            companion object {
                        fun fromJson(json: JSONObject): ScoreEntry = ScoreEntry(
                                        score = json.getInt("score"), status = json.getString("status"), timestamp = json.getLong("timestamp")
                        )
            }
                fun toJson(): JSONObject = JSONObject().apply {
                            put("score", score); put("status", status); put("timestamp", timestamp)
                }
}

class ScoreHistoryStore(context: Context) {
        private val prefs = context.getSharedPreferences("aura_score_history", Context.MODE_PRIVATE)
            private val maxEntries = 50

                fun saveScore(score: Int, status: String) {
                            val entries = getScores().toMutableList()
                                    entries.add(ScoreEntry(score, status, System.currentTimeMillis()))
                                            val trimmed = entries.takeLast(maxEntries)
                                                    val arr = JSONArray()
                                                            trimmed.forEach { arr.put(it.toJson()) }
                                                                    prefs.edit().putString("scores", arr.toString()).apply()
                }

                    fun getScores(): List<ScoreEntry> = runCatching {
                                val raw = prefs.getString("scores", null) ?: return emptyList()
                                        val arr = JSONArray(raw)
                                                (0 until arr.length()).map { ScoreEntry.fromJson(arr.getJSONObject(it)) }
                    }.getOrDefault(emptyList())

                        fun getTrend(): String {
                                    val scores = getScores().takeLast(5)
                                            return when {
                                                            scores.size < 2 -> "Estable"
                                                                        scores.last().score > scores.first().score + 5 -> "Mejorando"
                                                                                    scores.last().score < scores.first().score - 5 -> "Empeorando"
                                                                                                else -> "Estable"
                                            }
                        }

                            fun getAverageScore(): Int {
                                        val scores = getScores()
                                                return if (scores.isEmpty()) -1 else scores.sumOf { it.score } / scores.size
                            }

                                fun clear() { prefs.edit().remove("scores").apply() }
}
