package at.crowdware.humanitytree.core

import at.crowdware.humanitytree.model.InnerNode
import at.crowdware.humanitytree.model.InsertOutcome
import at.crowdware.humanitytree.model.LeafEntry
import at.crowdware.humanitytree.model.LeafNode
import at.crowdware.humanitytree.model.TreeNode

class HumanityTree(
    private val store: HumanityTreeStore,
    private val ipnsName: String = "humanitytree-root",
    private val defaultLeafCapacity: Int = 128
) {
    fun lookup(tag: String): List<String> {
        val rootCid = store.resolveIpns(ipnsName) ?: return emptyList()
        val node = store.loadNode(rootCid) ?: return emptyList()
        return lookupIn(node, tag)
    }

    fun insert(tag: String, profileCid: String): InsertOutcome {
        val rootCid = store.resolveIpns(ipnsName)
        val result = if (rootCid == null) {
            val leaf = LeafNode(maxEntries = defaultLeafCapacity, entries = listOf(LeafEntry(tag, listOf(profileCid))))
            val cid = store.saveNode(leaf)
            NodeWriteResult(cid, cid, true)
        } else {
            insertIntoNode(rootCid, tag, profileCid)
        }

        store.updateIpns(ipnsName, result.cid)
        return InsertOutcome(rootCid = result.cid, leafCid = result.leafCid, addedProfileCid = result.added)
    }

    private fun lookupIn(node: TreeNode, tag: String): List<String> {
        return when (node) {
            is LeafNode -> node.entries.firstOrNull { it.tag == tag }?.profileCids ?: emptyList()
            is InnerNode -> {
                val nextCid = if (tag < node.splitKey) node.leftCid else node.rightCid
                val next = store.loadNode(nextCid) ?: return emptyList()
                lookupIn(next, tag)
            }
            else -> emptyList()
        }
    }

    private fun insertIntoNode(cid: String, tag: String, profileCid: String): NodeWriteResult {
        val node = store.loadNode(cid) ?: error("Unknown CID: $cid")
        return when (node) {
            is LeafNode -> insertIntoLeaf(node, tag, profileCid)
            is InnerNode -> insertIntoInner(node, tag, profileCid)
            else -> error("Unsupported node type ${node::class.simpleName}")
        }
    }

    private fun insertIntoInner(node: InnerNode, tag: String, profileCid: String): NodeWriteResult {
        val goLeft = tag < node.splitKey
        val childCid = if (goLeft) node.leftCid else node.rightCid
        val childResult = insertIntoNode(childCid, tag, profileCid)
        val updated = if (goLeft) node.copy(leftCid = childResult.cid) else node.copy(rightCid = childResult.cid)
        val newCid = store.saveNode(updated)
        return NodeWriteResult(cid = newCid, leafCid = childResult.leafCid, added = childResult.added)
    }

    private fun insertIntoLeaf(leaf: LeafNode, tag: String, profileCid: String): NodeWriteResult {
        val (entries, added) = mergeEntries(leaf.entries, tag, profileCid)
        if (entries.size <= leaf.maxEntries) {
            val updated = leaf.copy(entries = entries)
            val cid = store.saveNode(updated)
            return NodeWriteResult(cid = cid, leafCid = cid, added = added)
        }

        val mid = entries.size / 2
        val leftEntries = entries.subList(0, mid)
        val rightEntries = entries.subList(mid, entries.size)

        val leftDraft = leaf.copy(entries = leftEntries, nextCid = null)
        val leftCidDraft = store.saveNode(leftDraft)

        val rightDraft = leaf.copy(entries = rightEntries, prevCid = leftCidDraft)
        val rightCidDraft = store.saveNode(rightDraft)

        val leftFinal = leftDraft.copy(nextCid = rightCidDraft)
        val leftCid = store.saveNode(leftFinal)

        val rightFinal = rightDraft.copy(prevCid = leftCid)
        val rightCid = store.saveNode(rightFinal)

        val splitKey = rightEntries.first().tag
        val promoted = InnerNode(splitKey = splitKey, leftCid = leftCid, rightCid = rightCid)
        val promotedCid = store.saveNode(promoted)
        val targetLeaf = if (tag < splitKey) leftCid else rightCid
        return NodeWriteResult(cid = promotedCid, leafCid = targetLeaf, added = added)
    }

    private fun mergeEntries(entries: List<LeafEntry>, tag: String, profileCid: String): Pair<List<LeafEntry>, Boolean> {
        val map = entries.associateBy { it.tag }.toMutableMap()
        val existing = map[tag]
        var added = false
        if (existing == null) {
            map[tag] = LeafEntry(tag = tag, profileCids = listOf(profileCid))
            added = true
        } else if (!existing.profileCids.contains(profileCid)) {
            map[tag] = existing.copy(profileCids = existing.profileCids + profileCid)
            added = true
        }
        val newEntries = map.values.sortedBy { it.tag }
        return newEntries to added
    }
}

private data class NodeWriteResult(
    val cid: String,
    val leafCid: String,
    val added: Boolean
)
