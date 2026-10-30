package com.sukabumi.tvlauncher

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class EspnScoreboard(val events: List<Event> = emptyList())

@Serializable
data class Event(
    val name: String = "",
    val competitions: List<Competition> = emptyList(),
    val status: Status? = null
)

@Serializable
data class Status(val type: StatusType? = null, val displayClock: String = "")

@Serializable
data class StatusType(val state: String = "", val detail: String = "", val description: String = "")

@Serializable
data class Competition(val competitors: List<Competitor> = emptyList(), val status: Status? = null)

@Serializable
data class Competitor(val team: Team? = null, val score: String = "0", val homeAway: String = "")

@Serializable
data class Team(val displayName: String = "", val abbreviation: String = "")

data class LiveMatch(
    val league: String,
    val home: String,
    val away: String,
    val homeScore: String,
    val awayScore: String,
    val status: String,
    val isLive: Boolean
)

object LiveScoreManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val LIGA1_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer/idn.1/scoreboard"
    private const val EPL_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard"

    suspend fun getLiveLiga1(): List<LiveMatch> = fetchMatches(LIGA1_URL, "Liga 1")
    suspend fun getLiveEPL(): List<LiveMatch> = fetchMatches(EPL_URL, "EPL")

    private fun fetchMatches(url: String, leagueName: String): List<LiveMatch> {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "SukabumiLauncher/3.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val scoreboard = json.decodeFromString<EspnScoreboard>(body)
            scoreboard.events.mapNotNull { event ->
                val comp = event.competitions.firstOrNull() ?: return@mapNotNull null
                val home = comp.competitors.find { it.homeAway == "home" }
                val away = comp.competitors.find { it.homeAway == "away" }
                val state = comp.status?.type?.state ?: event.status?.type?.state ?: "pre"
                val detail = comp.status?.type?.detail ?: event.status?.type?.detail ?: ""
                LiveMatch(
                    league = leagueName,
                    home = home?.team?.abbreviation ?: home?.team?.displayName ?: "HOME",
                    away = away?.team?.abbreviation ?: away?.team?.displayName ?: "AWAY",
                    homeScore = home?.score ?: "0",
                    awayScore = away?.score ?: "0",
                    status = when (state) {
                        "in" -> "LIVE $detail"
                        "post" -> "FT $detail"
                        else -> detail.ifEmpty { "Akan Datang" }
                    },
                    isLive = state == "in"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun shouldRefresh(lastFetchMillis: Long): Boolean {
        return System.currentTimeMillis() - lastFetchMillis > 60_000
    }
}
