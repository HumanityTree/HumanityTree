package at.crowdware.humanitytree.model

data class LeafEntry(
    val tag: String,
    val profileCids: List<String>
)

sealed interface TreeNode

data class LeafNode(
    val version: Int = 1,
    val maxEntries: Int = 128,
    val entries: List<LeafEntry>,
    val prevCid: String? = null,
    val nextCid: String? = null
) : TreeNode {
    fun firstTag(): String? = entries.minByOrNull { it.tag }?.tag
}

data class InnerNode(
    val version: Int = 1,
    val splitKey: String,
    val leftCid: String,
    val rightCid: String
) : TreeNode

data class GenesisRecord(
    val version: Int = 1,
    val createdAt: String,
    val description: String,
    val projectId: String = "innerNet",
    val schemaVersion: Int = 1,
    val leafSchemaDescriptionCid: String? = null,
    val publicProfileSchemaDescriptionCid: String? = null
)

data class RootDescriptor(
    val version: Int = 1,
    val currentRootCid: String,
    val createdAt: String,
    val note: String? = null
)

data class InsertOutcome(
    val rootCid: String,
    val leafCid: String,
    val addedProfileCid: Boolean
)
