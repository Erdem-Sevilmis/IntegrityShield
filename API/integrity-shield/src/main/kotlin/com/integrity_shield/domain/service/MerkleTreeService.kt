package com.integrity_shield.domain.service

import com.integrity_shield.crypto.CryptoUtils
import com.integrity_shield.domain.model.HashPosition
import com.integrity_shield.domain.model.ProofElement

class MerkleTreeService(
    private val leaves: MutableList<String> = mutableListOf(),
    private var root: String? = null,
    private val tree: MutableList<MutableList<String>> = mutableListOf()
) {
    fun leafCount(): Long = leaves.size.toLong()

    fun getRoot(): String = root ?: CryptoUtils.emptyHash()

    fun getLeaves(): List<String> = leaves.toList()

    fun append(leafHash: String) {
        require(leafHash.isNotBlank()) { "leafHash must not be blank" }
        leaves.add(leafHash)
        rebuildTree()
    }

    private fun rebuildTree() {
        tree.clear()
        tree.add(leaves.toMutableList())

        var level = 0
        while (tree[level].size > 1) {
            val currentLevel = tree[level]
            val nextLevel = mutableListOf<String>()

            for (i in currentLevel.indices step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) {
                    currentLevel[i + 1]
                } else {
                    CryptoUtils.emptyHash()
                }
                val parent = CryptoUtils.sha256HexConcat(left, right)
                nextLevel.add(parent)
            }

            tree.add(nextLevel)
            level++
        }

        root = if (tree.last().isNotEmpty()) {
            tree.last()[0]
        } else {
            CryptoUtils.emptyHash()
        }
    }

    fun generateProof(leafIndex: Long): List<ProofElement> {
        require(leafIndex >= 0 && leafIndex < leaves.size) {
            "Leaf index $leafIndex out of bounds [0, ${leaves.size})"
        }

        val proof = mutableListOf<ProofElement>()
        var currentIndex = leafIndex.toInt()

        for (level in 0 until tree.size - 1) {
            val isLeftChild = currentIndex % 2 == 0
            val siblingIndex = if (isLeftChild) {
                currentIndex + 1
            } else {
                currentIndex - 1
            }

            val sibling = if (siblingIndex < tree[level].size) {
                tree[level][siblingIndex]
            } else {
                CryptoUtils.emptyHash()
            }

            val position = if (isLeftChild) HashPosition.RIGHT else HashPosition.LEFT
            proof.add(ProofElement(sibling, position))

            currentIndex /= 2
        }

        return proof
    }

    fun verifyProof(leafHash: String, leafIndex: Long, proofPath: List<ProofElement>): Boolean {
        if (leafIndex < 0 || leafIndex >= leaves.size) {
            return false
        }

        var computedHash = leafHash
        var currentIndex = leafIndex.toInt()

        for (element in proofPath) {
            computedHash = when (element.position) {
                HashPosition.LEFT -> {
                    CryptoUtils.sha256HexConcat(element.siblingHash, computedHash)
                }
                HashPosition.RIGHT -> {
                    CryptoUtils.sha256HexConcat(computedHash, element.siblingHash)
                }
            }
            currentIndex /= 2
        }

        return computedHash == getRoot()
    }

    fun getDepth(): Int = tree.size
}

