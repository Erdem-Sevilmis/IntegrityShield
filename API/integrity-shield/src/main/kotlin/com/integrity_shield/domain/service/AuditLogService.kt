package com.integrity_shield.domain.service

import com.integrity_shield.crypto.CryptoUtils
import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.ProofElement
import java.time.Instant

/**
 * Core audit logic: append file versions to the Merkle Tree, verify inclusion,
 * generate proofs, and reconstruct tree state from persistence.
 *
 * This is a pure domain service with NO Spring or framework dependencies.
 */
class AuditLogService(
    private var merkleTreeService: MerkleTreeService = MerkleTreeService()
) {

    /**
     * Derives the leaf hash for a file version using SHA-256.
     *
     * Leaf hash = SHA-256(fileIdentifier + storageVersionId + contentHash + uploadTimestamp)
     *
     * Note: metadata is intentionally excluded from the hash calculation.
     * Only deterministic, client-known values are included so that the
     * verifier can always reproduce the exact same hash.
     */
    fun deriveLeafHash(
        fileIdentifier: String,
        storageVersionId: String,
        contentHash: String,
        uploadTimestamp: Instant
    ): String {
        return CryptoUtils.sha256HexConcat(
            fileIdentifier,
            storageVersionId,
            contentHash,
            uploadTimestamp.toString()
        )
    }

    /**
     * Appends a leaf hash to the Merkle Tree and returns the new leaf index.
     */
    fun appendLeaf(leafHash: String): Long {
        require(leafHash.isNotBlank()) { "leafHash must not be blank" }
        merkleTreeService.append(leafHash)
        return merkleTreeService.leafCount() - 1
    }

    /**
     * Returns the current Merkle root hash.
     */
    fun getCurrentRoot(): String = merkleTreeService.getRoot()

    /**
     * Returns the current number of leaves in the tree.
     */
    fun getCurrentLeafCount(): Long = merkleTreeService.leafCount()

    /**
     * Generates a Merkle proof for a given leaf index.
     * Returns null if the leaf index is out of bounds.
     */
    fun generateProof(leafIndex: Long): List<ProofElement>? {
        return try {
            merkleTreeService.generateProof(leafIndex)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Verifies that a leaf is included in the Merkle Tree by:
     * 1. Re-deriving the leaf hash from the input parameters
     * 2. Generating a proof path for the leaf
     * 3. Verifying the proof against the known root
     *
     * Returns true only if all steps succeed and the computed root matches knownRoot.
     */
    fun verifyInclusion(
        leafHash: String,
        leafIndex: Long,
        knownRoot: String
    ): Boolean {
        val proofPath = generateProof(leafIndex) ?: return false
        val proofValid = merkleTreeService.verifyProof(leafHash, leafIndex, proofPath)
        if (!proofValid) return false

        val currentRoot = merkleTreeService.getRoot()
        return currentRoot == knownRoot
    }

    /**
     * Reconstructs the entire Merkle Tree from a list of persisted file versions.
     * Versions must be sorted by merkleLeafIndex ascending.
     */
    fun reconstructFromFileVersions(versions: List<DomainFileVersion>) {
        merkleTreeService = MerkleTreeService()
        val sorted = versions.sortedBy { it.merkleLeafIndex }
        for (version in sorted) {
            merkleTreeService.append(version.leafHash)
        }
    }
}

