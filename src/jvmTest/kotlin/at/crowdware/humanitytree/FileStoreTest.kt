package at.crowdware.humanitytree

import at.crowdware.humanitytree.core.FileStore
import at.crowdware.humanitytree.core.HumanityTree
import at.crowdware.humanitytree.model.LeafEntry
import at.crowdware.humanitytree.model.LeafNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import java.nio.file.Path

class FileStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun persistsNodesAcrossInstances() {
        val storeA = FileStore(tempDir)
        val treeA = HumanityTree(storeA, defaultLeafCapacity = 2)
        treeA.insert("#A", "cidA")
        treeA.insert("#B", "cidB")

        val rootCid = storeA.resolveIpns("humanitytree-root")
        assertNotNull(rootCid)

        val storeB = FileStore(tempDir)
        val treeB = HumanityTree(storeB, defaultLeafCapacity = 2)
        val lookupA = treeB.lookup("#A")
        val lookupB = treeB.lookup("#B")

        assertEquals(listOf("cidA"), lookupA)
        assertEquals(listOf("cidB"), lookupB)
    }

    @Test
    fun savesNodeContent() {
        val store = FileStore(tempDir)
        val leaf = LeafNode(entries = listOf(LeafEntry(tag = "#X", profileCids = listOf("p1"))))
        val cid = store.saveNode(leaf)
        val loaded = store.loadNode(cid) as LeafNode
        assertEquals("#X", loaded.entries.first().tag)
    }
}
