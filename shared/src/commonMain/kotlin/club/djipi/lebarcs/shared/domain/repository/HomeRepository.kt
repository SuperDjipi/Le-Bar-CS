package club.djipi.lebarcs.shared.domain.repository

import club.djipi.lebarcs.shared.domain.model.PlayerGameSummary

interface HomeRepository {

suspend fun getMyActiveGames(playerId: String): List<PlayerGameSummary>

}
