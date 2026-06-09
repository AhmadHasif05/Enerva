package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserDirectoryEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest : RoomDaoTest() {

    private lateinit var dao: UserDao

    @Before
    fun initDao() {
        dao = db.userDao()
    }

    private fun user(
        email: String,
        name: String = email.substringBefore('@'),
        uid: String? = null
    ) = UserEntity(
        email = email,
        runnerName = name,
        location = "",
        fitnessLevel = "",
        personalGoal = "",
        bio = "",
        following = 0,
        followers = 0,
        firebaseUid = uid
    )

    @Test
    fun upsert_then_findByEmail_returns_the_row() = runTest {
        dao.upsertUser(user("me@example.com", "Hasif"))
        assertEquals("Hasif", dao.findByEmail("me@example.com")?.runnerName)
    }

    @Test
    fun upsert_replaces_an_existing_row_with_the_same_email() = runTest {
        dao.upsertUser(user("me@example.com", "Hasif"))
        dao.upsertUser(user("me@example.com", "Hasif Renamed"))
        assertEquals("Hasif Renamed", dao.findByEmail("me@example.com")?.runnerName)
    }

    @Test
    fun findByFirebaseUid_maps_the_cloud_id_to_the_local_row() = runTest {
        dao.upsertUser(user("me@example.com", "Hasif", uid = "uid-123"))
        assertEquals("me@example.com", dao.findByFirebaseUid("uid-123")?.email)
        assertNull(dao.findByFirebaseUid("nope"))
    }

    @Test
    fun setFirebaseUid_backfills_the_uid() = runTest {
        dao.upsertUser(user("me@example.com"))
        dao.setFirebaseUid("me@example.com", "uid-xyz")
        assertEquals("uid-xyz", dao.findByEmail("me@example.com")?.firebaseUid)
    }

    @Test
    fun observeAllExcept_drops_me_and_sorts_by_name() = runTest {
        dao.upsertUser(user("me@example.com", "Me"))
        dao.upsertUser(user("zoe@example.com", "Zoe"))
        dao.upsertUser(user("amy@example.com", "Amy"))

        val others = dao.observeAllExcept("me@example.com").first()

        assertEquals(listOf("Amy", "Zoe"), others.map { it.runnerName })
    }

    @Test
    fun follows_are_observed_counted_and_removed() = runTest {
        dao.addFollow(FollowEntity("me@example.com", "Liyana"))
        dao.addFollow(FollowEntity("me@example.com", "Daniel"))
        dao.addFollow(FollowEntity("other@example.com", "Liyana"))

        assertEquals(setOf("Liyana", "Daniel"), dao.observeFollowing("me@example.com").first().toSet())
        // Liyana is followed by two owners.
        assertEquals(2, dao.countFollowersOf("Liyana"))

        dao.removeFollow("me@example.com", "Liyana")
        assertEquals(listOf("Daniel"), dao.observeFollowing("me@example.com").first())
        assertEquals(1, dao.countFollowersOf("Liyana"))
    }

    @Test
    fun renameFollowFriend_repoints_follow_rows_to_the_new_name() = runTest {
        dao.addFollow(FollowEntity("me@example.com", "Old Name"))
        dao.renameFollowFriend("Old Name", "New Name")
        assertEquals(listOf("New Name"), dao.observeFollowing("me@example.com").first())
    }

    @Test
    fun directory_users_are_mirrored_and_exclude_me() = runTest {
        dao.upsertDirectoryUsers(
            listOf(
                UserDirectoryEntity("uid-me", "Me", "", ""),
                UserDirectoryEntity("uid-amy", "Amy", "Penang", "Intermediate"),
            )
        )

        val directory = dao.observeDirectoryExcept("uid-me").first()
        assertEquals(listOf("Amy"), directory.map { it.runnerName })
        assertEquals("Penang", dao.findDirectoryByName("Amy")?.location)
    }
}
