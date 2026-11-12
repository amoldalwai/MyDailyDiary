package com.amoldalwai.diary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.FinishInfo
import java.io.File

/** The book library: the shared media library fed with books. */
@Composable
fun BooksScreen(
    books: List<Book>,
    cover: (String) -> File,
    onAddBook: (BookResult, BookStatus, Int?, Int, FinishInfo?) -> Unit,
    onUpdate: (Book) -> Unit,
    onDelete: (Book) -> Unit,
    onBack: () -> Unit,
) {
    val labels = mapOf(BookStatus.Reading to "Reading", BookStatus.Finished to "Finished", BookStatus.Want to "Want to read")
    val shelves = remember { listOf(BookStatus.Reading, BookStatus.Finished, BookStatus.Want).map { MediaShelf(it.name, labels.getValue(it)) } }
    val items = remember(books) {
        books.map {
            MediaItem(
                id = it.id,
                shelf = it.status.name,
                title = it.title,
                subtitle = it.authors,
                cover = it.cover?.let(cover),
                rating = it.rating,
                progress = if (it.status == BookStatus.Reading && it.totalPages != null) it.progress else null,
            )
        }
    }
    val finished = books.count { it.status == BookStatus.Finished }

    MediaLibraryScreen(
        title = "Book library",
        emoji = "📚",
        summary = "${books.size} ${if (books.size == 1) "book" else "books"} · $finished finished",
        shelves = shelves,
        items = items,
        emptyTitle = { shelf ->
            when (shelf?.key) {
                BookStatus.Reading.name -> "Nothing on the go"
                BookStatus.Finished.name -> "No finished books yet"
                BookStatus.Want.name -> "Your reading list is empty"
                else -> "Your library is empty"
            }
        },
        emptyHint = "Tap + to search for a book.",
        onBack = onBack,
        addSheet = { dismiss -> BookSearchSheet(onDismiss = dismiss, onAdd = onAddBook) },
        detailSheet = { id, dismiss ->
            books.firstOrNull { it.id == id }?.let { book ->
                BookDetailSheet(
                    book = book,
                    cover = cover,
                    onDismiss = dismiss,
                    onSave = onUpdate,
                    onDelete = { onDelete(book) },
                )
            }
        },
    )
}
