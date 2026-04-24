// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract MerkleRootAnchor {

    address public owner;
    mapping(bytes32 => uint256) public anchoredRoots;

    event MerkleRootAnchored(
        bytes32 indexed merkleRoot,
        uint256 indexed blockNumber,
        uint256 timestamp,
        address indexed anchorer
    );

    event OwnershipTransferred(
        address indexed previousOwner,
        address indexed newOwner
    );

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function anchorMerkleRoot(bytes32 merkleRoot) external onlyOwner {
        require(merkleRoot != bytes32(0), "Merkle root cannot be empty");
        require(anchoredRoots[merkleRoot] == 0, "Root already anchored");

        anchoredRoots[merkleRoot] = block.timestamp;

        emit MerkleRootAnchored(
            merkleRoot,
            block.number,
            block.timestamp,
            msg.sender
        );
    }

    function isRootAnchored(bytes32 merkleRoot) external view returns (bool) {
        return anchoredRoots[merkleRoot] > 0;
    }

    function getAnchorTimestamp(bytes32 merkleRoot) external view returns (uint256) {
        return anchoredRoots[merkleRoot];
    }

    function transferOwnership(address newOwner) external onlyOwner {
        require(newOwner != address(0), "New owner cannot be zero address");
        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }
}