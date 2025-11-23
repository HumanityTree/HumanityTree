package at.crowdware.humanitytree

import at.crowdware.humanitytree.core.HumanityTree
import at.crowdware.humanitytree.core.InMemoryStore
import at.crowdware.humanitytree.model.InnerNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HumanityTreeTest {
    @Test
    fun insertAndLookupMultipleProfiles() {
        val store = InMemoryStore()
        val tree = HumanityTree(store, defaultLeafCapacity = 4)

        tree.insert("#Guitarist", "cid1")
        tree.insert("#Guitarist", "cid2")

        val results = tree.lookup("#Guitarist")
        assertEquals(listOf("cid1", "cid2"), results)
        assertNotNull(store.resolveIpns("humanitytree-root"))
    }

    @Test
    fun splitsLeafWhenCapacityExceeded() {
        val store = InMemoryStore()
        val tree = HumanityTree(store, defaultLeafCapacity = 2)

        tree.insert("#B", "cidB")
        tree.insert("#C", "cidC")
        tree.insert("#A", "cidA")

        val rootCid = store.resolveIpns("humanitytree-root")
        assertNotNull(rootCid)
        val rootNode = store.loadNode(rootCid!!)
        assertTrue(rootNode is InnerNode, "Root should be promoted to InnerNode after split")

        assertEquals(listOf("cidA"), tree.lookup("#A"))
        assertEquals(listOf("cidB"), tree.lookup("#B"))
        assertEquals(listOf("cidC"), tree.lookup("#C"))
    }
}
