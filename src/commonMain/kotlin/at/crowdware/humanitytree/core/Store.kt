package at.crowdware.humanitytree.core

import at.crowdware.humanitytree.model.TreeNode

interface HumanityTreeStore {
    fun loadNode(cid: String): TreeNode?
    fun saveNode(node: TreeNode): String
    fun resolveIpns(name: String): String?
    fun updateIpns(name: String, cid: String)
}

class InMemoryStore(
    private val codec: NodeCodec = NodeCodec()
) : HumanityTreeStore {
    private val nodes = mutableMapOf<String, String>()
    private val ipns = mutableMapOf<String, String>()

    override fun loadNode(cid: String): TreeNode? = nodes[cid]?.let { codec.decode(it) }

    override fun saveNode(node: TreeNode): String {
        val content = codec.encode(node)
        val cid = cidFrom(content)
        nodes[cid] = content
        return cid
    }

    override fun resolveIpns(name: String): String? = ipns[name]

    override fun updateIpns(name: String, cid: String) {
        ipns[name] = cid
    }

    private fun cidFrom(content: String): String {
        val hash = content.hashCode().toUInt().toString(16)
        return "cid-$hash"
    }
}
