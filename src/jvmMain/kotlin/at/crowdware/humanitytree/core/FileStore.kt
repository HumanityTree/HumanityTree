package at.crowdware.humanitytree.core

import at.crowdware.humanitytree.model.TreeNode
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * File-backed HumanityTreeStore. Stores node SML documents under baseDir using CID as filename
 * and tracks IPNS name mappings in a simple text file per name.
 */
class FileStore(
    private val baseDir: Path,
    private val codec: NodeCodec = NodeCodec()
) : HumanityTreeStore {

    private val nodesDir: Path = baseDir.resolve("nodes")
    private val ipnsDir: Path = baseDir.resolve("ipns")

    init {
        Files.createDirectories(nodesDir)
        Files.createDirectories(ipnsDir)
    }

    override fun loadNode(cid: String): TreeNode? {
        val path = nodesDir.resolve("$cid.sml")
        if (!Files.exists(path)) return null
        val content = Files.readString(path)
        return codec.decode(content)
    }

    override fun saveNode(node: TreeNode): String {
        val content = codec.encode(node)
        val cid = sha256Cid(content)
        val path = nodesDir.resolve("$cid.sml")
        if (!Files.exists(path)) {
            Files.writeString(path, content)
        }
        return cid
    }

    override fun resolveIpns(name: String): String? {
        val path = ipnsDir.resolve("$name.txt")
        return if (Files.exists(path)) Files.readString(path).trim().ifEmpty { null } else null
    }

    override fun updateIpns(name: String, cid: String) {
        val path = ipnsDir.resolve("$name.txt")
        Files.writeString(path, cid)
    }

    private fun sha256Cid(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
