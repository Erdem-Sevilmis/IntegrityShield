package com.integrity_shield.domain.exception

class RootAlreadyAnchoredException(val rootHash: String) :
    RuntimeException("Merkle root $rootHash has already been anchored on-chain")

