package com.amoldalwai.diary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameResult
import com.amoldalwai.diary.data.GameStatus
import java.io.File

/** The game library: the shared media library fed with games. */
@Composable
fun GamesScreen(
    games: List<Game>,
    cover: (String) -> File,
    onAddGame: (GameResult, String, GameStatus) -> Unit,
    onUpdate: (Game) -> Unit,
    onDelete: (Game) -> Unit,
    onBack: () -> Unit,
) {
    val labels = mapOf(GameStatus.Playing to "Playing", GameStatus.Completed to "Completed", GameStatus.Backlog to "Backlog")
    val shelves = remember { listOf(GameStatus.Playing, GameStatus.Completed, GameStatus.Backlog).map { MediaShelf(it.name, labels.getValue(it)) } }
    val items = remember(games) {
        games.map {
            MediaItem(
                id = it.id,
                shelf = it.status.name,
                title = it.title,
                subtitle = it.platform,
                cover = it.cover?.let(cover),
                rating = it.rating,
                badge = it.platform,
            )
        }
    }
    val completed = games.count { it.status == GameStatus.Completed }

    MediaLibraryScreen(
        title = "Game library",
        emoji = "🎮",
        summary = "${games.size} ${if (games.size == 1) "game" else "games"} · $completed completed",
        shelves = shelves,
        items = items,
        emptyTitle = { shelf ->
            when (shelf?.key) {
                GameStatus.Playing.name -> "Nothing on the go"
                GameStatus.Completed.name -> "No completed games yet"
                GameStatus.Backlog.name -> "Your backlog is empty"
                else -> "Your game library is empty"
            }
        },
        emptyHint = "Tap + to add a game.",
        onBack = onBack,
        addSheet = { dismiss -> GameSearchSheet(onDismiss = dismiss, onAdd = onAddGame) },
        detailSheet = { id, dismiss ->
            games.firstOrNull { it.id == id }?.let { game ->
                GameDetailSheet(
                    game = game,
                    cover = cover,
                    onDismiss = dismiss,
                    onSave = onUpdate,
                    onDelete = { onDelete(game) },
                )
            }
        },
    )
}
