package at.crowdware.humanitytree

import at.crowdware.humanitytree.core.FileStore
import at.crowdware.humanitytree.core.HumanityTree
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.random.Random

class LoadSimulationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun concurrentBotsInsertAndLookup() {
        val store = FileStore(tempDir)
        val tree = HumanityTree(store, defaultLeafCapacity = 64)

        val tags = List(2000) { "#Tag$it" }
        val profiles = List(1000) { "profile-$it" }

        val bots = 8
        val opsPerBot = 2000
        val executor = Executors.newFixedThreadPool(bots)
        val latch = CountDownLatch(bots)

        repeat(bots) { idx ->
            executor.submit {
                val rnd = Random(idx + 1)
                repeat(opsPerBot) {
                    val tag = tags[rnd.nextInt(tags.size)]
                    val cid = profiles[rnd.nextInt(profiles.size)]
                    // Serialize tree mutation to keep the simple store safe under test load
                    synchronized(tree) { tree.insert(tag, cid) }
                    if (rnd.nextDouble() < 0.3) {
                        tree.lookup(tag)
                    }
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        // After concurrent ops, ensure at least one tag has entries and lookups succeed.
        val sampleResults = tree.lookup(tags.first())
        assertTrue(sampleResults.isNotEmpty())
    }
}
