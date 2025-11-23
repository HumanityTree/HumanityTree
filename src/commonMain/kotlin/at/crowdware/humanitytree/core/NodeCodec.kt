package at.crowdware.humanitytree.core

import at.crowdware.humanitytree.model.*
import at.crowdware.humanitytree.sml.SmlDomParser
import at.crowdware.humanitytree.sml.SmlElement
import at.crowdware.humanitytree.sml.int
import at.crowdware.humanitytree.sml.string
import at.crowdware.sml.PropertyValue

/** Serializes HumanityTree nodes to SML and decodes them back. */
class NodeCodec {
    fun encode(node: TreeNode): String = when (node) {
        is LeafNode -> encodeLeaf(node)
        is InnerNode -> encodeInner(node)
        else -> error("Unsupported node type: ${node::class.simpleName}")
    }

    fun decode(text: String): TreeNode {
        val root = SmlDomParser.parse(text).firstOrNull()
            ?: error("Empty SML document")
        return when (root.name) {
            "Leaf" -> decodeLeaf(root)
            "InnerNode" -> decodeInner(root)
            else -> error("Unknown root element ${root.name}")
        }
    }

    private fun encodeLeaf(node: LeafNode): String = buildString {
        appendLine("Leaf {")
        appendLine("  type: \"Leaf\"")
        appendLine("  version: ${node.version}")
        appendLine("  maxEntries: ${node.maxEntries}")
        node.prevCid?.let { appendLine("  PrevCid: \"$it\"") }
        node.nextCid?.let { appendLine("  NextCid: \"$it\"") }
        node.entries.forEach { entry ->
            appendLine("  Entry {")
            appendLine("    tag: \"${entry.tag}\"")
            entry.profileCids.forEach { cid ->
                appendLine("    Profile {")
                appendLine("      cid: \"$cid\"")
                appendLine("    }")
            }
            appendLine("  }")
        }
        appendLine("}")
    }

    private fun encodeInner(node: InnerNode): String = buildString {
        appendLine("InnerNode {")
        appendLine("  type: \"InnerNode\"")
        appendLine("  version: ${node.version}")
        appendLine("  splitKey: \"${node.splitKey}\"")
        appendLine("  LeftCid: \"${node.leftCid}\"")
        appendLine("  RightCid: \"${node.rightCid}\"")
        appendLine("}")
    }

    private fun decodeLeaf(el: SmlElement): LeafNode {
        val maxEntries = el.int("maxEntries") ?: 128
        val prevCid = el.string("PrevCid")?.takeIf { it.isNotEmpty() }
        val nextCid = el.string("NextCid")?.takeIf { it.isNotEmpty() }
        val entries = el.children.filter { it.name == "Entry" }.map { entry ->
            val tag = entry.string("tag") ?: error("Entry missing tag")
            val profiles = entry.children.filter { it.name == "Profile" }
                .mapNotNull { it.string("cid") }
            LeafEntry(tag = tag, profileCids = profiles)
        }
        return LeafNode(maxEntries = maxEntries, entries = entries, prevCid = prevCid, nextCid = nextCid)
    }

    private fun decodeInner(el: SmlElement): InnerNode {
        val splitKey = el.string("splitKey") ?: error("InnerNode missing splitKey")
        val leftCid = el.string("LeftCid") ?: error("InnerNode missing LeftCid")
        val rightCid = el.string("RightCid") ?: error("InnerNode missing RightCid")
        return InnerNode(splitKey = splitKey, leftCid = leftCid, rightCid = rightCid)
    }
}
