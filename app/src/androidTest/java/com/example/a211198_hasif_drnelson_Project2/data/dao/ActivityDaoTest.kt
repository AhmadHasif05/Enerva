package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityDaoTest : RoomDaoTest() {

    private lateinit var dao: ActivityDao

    @Before
    fun initDao() {
        dao = db.activityDao()
    }

    private fun media(
        id: String,
        owner: String,
        author: String,
        createdAt: Long
    ) = MediaEntity(
        id = id,
        ownerEmail = owner,
        author = author,
        caption = "c",
        activity = "Run",
        distanceKm = "5",
        tint = 0L,
        imageRes = 0,
        imageUri = null,
        likes = 0,
        createdAtMs = createdAt
    )

    @Test
    fun observeMedia_is_owner_scoped_and_newest_first() = runTest {
        dao.insertMedia(media("a", "me@example.com", "Me", createdAt = 100))
        dao.insertMedia(media("b", "me@example.com", "Me", createdAt = 300))
        dao.insertMedia(media("c", "other@example.com", "Other", createdAt = 200))

        val mine = dao.observeMedia("me@example.com").first()
        assertEquals(listOf("b", "a"), mine.map { it.id }) // newest (300) first
        assertEquals(2, dao.countMedia("me@example.com"))
        assertEquals(1, dao.countMedia("other@example.com"))
    }

    @Test
    fun observeAllMedia_returns_every_owner_newest_first() = runTest {
        dao.insertMedia(media("a", "me@example.com", "Me", createdAt = 100))
        dao.insertMedia(media("c", "uid:remote", "Remote", createdAt = 300))

        val feed = dao.observeAllMedia().first()
        assertEquals(listOf("c", "a"), feed.map { it.id })
    }

    @Test
    fun observeMediaByAuthor_filters_on_display_name() = runTest {
        dao.insertMedia(media("a", "me@example.com", "Amy", createdAt = 100))
        dao.insertMedia(media("b", "me@example.com", "Bob", createdAt = 200))

        assertEquals(listOf("a"), dao.observeMediaByAuthor("Amy").first().map { it.id })
        assertEquals(1, dao.countMediaByAuthor("Bob"))
    }

    @Test
    fun insertAllMedia_seeds_in_bulk() = runTest {
        dao.insertAllMedia(
            listOf(
                media("a", "me@example.com", "Me", createdAt = 100),
                media("b", "me@example.com", "Me", createdAt = 200),
            )
        )
        assertEquals(2, dao.countMedia("me@example.com"))
    }

    @Test
    fun renameAuthor_repoints_existing_reels() = runTest {
        dao.insertMedia(media("a", "me@example.com", "Old Name", createdAt = 100))
        dao.renameAuthor("Old Name", "New Name")

        assertEquals(0, dao.countMediaByAuthor("Old Name"))
        assertEquals(1, dao.countMediaByAuthor("New Name"))
    }
}
