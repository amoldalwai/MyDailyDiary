package com.amoldalwai.diary.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lead sentences copied from Wikipedia film articles. */
class ScreenSearchGenresTest {
    private fun genres(sentence: String) = ScreenSearch.genresFrom(sentence)

    @Test fun scienceFictionHeist() =
        assertEquals(listOf("Sci-fi", "Heist"), genres("Inception is a 2010 science fiction heist film written and directed by Christopher Nolan."))

    @Test fun biographicalSportsDrama() =
        assertEquals(listOf("Biography", "Sports", "Drama"), genres("Dangal (transl. 'wrestling bout') is a 2016 Indian Hindi-language biographical sports drama film directed by Nitesh Tiwari."))

    @Test fun mythologicalActionDrama() =
        assertEquals(listOf("Mythological", "Action", "Drama"), genres("Kantara is a 2022 Indian Kannada-language mythological  action drama film written and directed by Rishab Shetty."))

    @Test fun romanticComedyIsOneGenre() =
        assertEquals(listOf("Romantic comedy", "Drama"), genres("Vadapav is a 2025 Indian Marathi-language romantic comedy drama film directed by Prasad Oak."))

    @Test fun lineBreakInsideTheClause() =
        assertEquals(listOf("Animation", "Sci-fi", "Thriller"), genres("Paprika is a 2006 Japanese animated surrealist \nscience fiction psychological thriller film directed by Satoshi Kon."))

    @Test fun noGenreNamed() =
        assertEquals(emptyList<String>(), genres("Dangal (transl. The Bout) is a 1977 Indian Bhojpuri-language film produced by Bachubhai Shah."))

    @Test fun notAFilmSentence() = assertEquals(emptyList<String>(), genres(""))
}
