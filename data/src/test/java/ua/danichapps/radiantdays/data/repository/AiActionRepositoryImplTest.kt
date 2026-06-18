package ua.danichapps.radiantdays.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.data.local.dao.AiActionDao
import ua.danichapps.radiantdays.data.local.seed.BuiltinAiActions

class AiActionRepositoryImplTest {

    private lateinit var dao: AiActionDao
    private lateinit var repository: AiActionRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = AiActionRepositoryImpl(dao)
    }

    @Test
    fun `ensureBuiltinActions inserts all built-in actions`() = runTest {
        coEvery { dao.countAll() } returns 0

        repository.ensureBuiltinActions()

        coVerify(exactly = 1) { dao.insertActions(BuiltinAiActions.all) }
        assertEquals(3, BuiltinAiActions.all.size)
    }

    @Test
    fun `ensureBuiltinActions is idempotent via INSERT OR IGNORE`() = runTest {
        repository.ensureBuiltinActions()
        repository.ensureBuiltinActions()

        coVerify(exactly = 2) { dao.insertActions(BuiltinAiActions.all) }
    }
}
