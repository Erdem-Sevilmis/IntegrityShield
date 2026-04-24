# IntegrityShield

> **Master's Thesis Project** — Cryptographic audit platform with Merkle Tree integrity proofs and Blockchain anchoring for tamper-proof file verification.

---

## Overview

IntegrityShield provides audit-proof cloud storage by combining three mechanisms:

1. **Immutable Storage** — Files are uploaded to AWS S3 with Object Lock (WORM), preventing deletion or modification.
2. **Merkle Tree Audit Log** — Every file version is hashed (SHA-256) and appended as a leaf to an off-chain Merkle Tree, enabling efficient cryptographic inclusion proofs.
3. **Blockchain Anchoring** — The Merkle root is periodically anchored on an Ethereum-compatible blockchain (Sepolia testnet), creating an immutable, publicly verifiable timestamp.

**Verification flow**: Given a file's metadata, the system regenerates the leaf hash, computes a Merkle proof path, and verifies it against a known (blockchain-anchored) root — proving the file existed in the audit log at anchoring time without trusting any single party.

---

## Architecture

The project follows a **Hexagonal Architecture (Ports & Adapters)** layered structure:

```
src/main/kotlin/com/integrity_shield/
├── domain/                  # Pure business logic, zero framework dependencies
│   ├── model/               #   Domain entities (GasEstimate, AnchoringResult, …)
│   ├── service/             #   MerkleTree, AuditLogService
│   └── port/                #   Interfaces (inbound use cases + outbound ports)
│       ├── inbound/         #     FileUploadUseCase, AuditVerificationUseCase, …
│       └── outbound/        #     FileStoragePort, BlockchainAnchorPort, GasPricePort, …
├── application/             # Use case implementations (orchestration layer)
│   ├── FileUploadUseCaseImpl
│   ├── AuditVerificationUseCaseImpl
│   ├── BlockchainAnchoringUseCaseImpl
│   └── GasEstimationUseCaseImpl
├── adapter/                 # Framework-specific code
│   ├── inbound/web/         #   REST controllers + DTOs
│   ├── outbound/persistence/#   JPA entities, repositories, mappers
│   ├── outbound/storage/    #   AWS S3 adapter
│   ├── outbound/blockchain/ #   Web3j Ethereum + gas price adapters
│   └── scheduler/           #   Scheduled blockchain anchoring
├── config/                  # Spring configuration (S3, Blockchain, Initializer)
└── crypto/                  # SHA-256 hashing utility
```

**Dependency rule**: `adapter → application → domain`. The domain layer has zero imports from Spring, Web3j, or AWS SDK.

### Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.2 / Java 17 |
| Framework | Spring Boot 4.0.1 |
| Database | H2 (in-memory, JPA) |
| Cloud Storage | AWS S3 (Object Lock / WORM) |
| Blockchain | Web3j 4.12.0, Ethereum Sepolia Testnet |
| Smart Contract | Solidity (`MerkleRootAnchor.sol`) |
| Cryptography | SHA-256, Merkle Tree proofs |

---

## Getting Started

### Prerequisites

- Java 17+
- Gradle 8.x (wrapper included)
- AWS S3 credentials (for file storage)
- Ethereum RPC endpoint + wallet private key (for blockchain anchoring)

### Configuration

Copy `.env.example` to `.env` and fill in your credentials. The application reads environment variables with fallback defaults.

**Required environment variables in `.env`**:

```dotenv
# AWS S3 Configuration
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
AWS_REGION=eu-north-1
AWS_S3_BUCKET_NAME=integrity-shield

# Blockchain Configuration (Sepolia Testnet)
BLOCKCHAIN_RPC_URL=https://sepolia.infura.io/v3/your-infura-key
BLOCKCHAIN_PRIVATE_KEY=your-ethereum-private-key
BLOCKCHAIN_CONTRACT_ADDRESS=0x9075B7e735E44b8CD5b9487628c4D6291a197056
BLOCKCHAIN_API_KEY_SECRET=:your-api-key-secret

# Mainnet RPC (read-only, for gas estimation)
BLOCKCHAIN_MAINNET_RPC_URL=https://mainnet.infura.io/v3/your-infura-key
```

Key configuration in `application.yaml`:

| Property | Description |
|---|---|
| `aws.credentials.access-key` / `secret-key` | AWS S3 credentials |
| `aws.s3.bucket-name` | S3 bucket name |
| `blockchain.network.rpcUrl` | Ethereum RPC endpoint (e.g. Sepolia via Infura) |
| `blockchain.wallet.private-key` | Wallet private key for signing transactions |
| `blockchain.contract.address` | Deployed `MerkleRootAnchor` contract address |
| `blockchain.scheduler.enabled` | `true` to enable periodic auto-anchoring |
| `blockchain.scheduler.interval-minutes` | Anchoring interval (default: 5) |


### Build & Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test
```

The API is available at `http://localhost:8080`.

---

## API Reference

Full OpenAPI 3.0 specification: [`openapi.yaml`](openapi.yaml)

### File Management

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/files/upload` | Upload a file (multipart) — stores in S3, computes hash, appends to Merkle Tree |

**Upload response** includes `id`, `fileIdentifier`, `storageVersionId`, `contentHash`, `leafIndex`, `uploadTimestamp`, `currentRoot`, and `leafCount`. Store `storageVersionId` and `uploadTimestamp` exactly as returned — they are needed for verification.

Duplicate uploads (same content hash) return HTTP 200 with `status: "idempotent"`.

### Audit & Verification

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/audit/current-root` | Get the current Merkle root hash and leaf count |
| `POST` | `/api/v1/audit/verify-inclusion` | Verify a file's inclusion in the audit log via Merkle proof |
| `GET` | `/api/v1/audit/proof/{fileVersionId}` | Retrieve the Merkle proof for a specific file version |
| `GET` | `/api/v1/audit/all-versions` | List all file versions in the audit log |
| `GET` | `/api/v1/audit/versions/{fileIdentifier}` | List all versions of a specific file |

**Verification request body**:
```json
{
  "fileIdentifier": "document.pdf",
  "storageVersionId": "v1-abc123def",
  "contentHash": "3a5e9a7c…",
  "uploadTimestamp": "2026-03-05T14:23:45.123Z",
  "knownRoot": "7f4a3c9b…"
}
```

The `knownRoot` should be a blockchain-anchored root hash for tamper-proof verification.

### Blockchain Anchoring

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/audit/anchor-root` | Anchor current Merkle root on blockchain |
| `GET` | `/api/v1/audit/latest-anchor` | Get the most recent anchoring result |
| `GET` | `/api/v1/audit/anchors/verify` | Verify an anchor on-chain (`?rootHash=…&transactionHash=…`) |
| `GET` | `/api/v1/audit/gas-estimate` | Estimate anchoring gas costs (testnet + mainnet projection) |

**Verifying an anchor on Etherscan**: After anchoring, visit `https://sepolia.etherscan.io/tx/{transactionHash}` → *More Details* → *Input Data* → *Decode as UTF-8* to see the anchored root hash.

### Gas Estimation

The `/api/v1/audit/gas-estimate` endpoint returns:
- **Testnet**: Current gas price, gas used, total cost in Wei/ETH/USD/EUR
- **Mainnet estimate**: Projected cost based on mainnet gas price
- **Exchange rates**: ETH/USD and ETH/EUR (via CoinGecko, cached 60s)

---

## Smart Contract

**`MerkleRootAnchor.sol`** (Solidity ^0.8.10) — deployed on Sepolia testnet at `0x9075B7e735E44b8CD5b9487628c4D6291a197056`:

- `anchorMerkleRoot(string merkleRoot)` — Stores the root hash with `block.timestamp`, emits `MerkleRootAnchored` event
- `getAnchor(string merkleRoot)` → `uint256` — Returns the timestamp of an anchored root (0 if not found)

**[View on Blockscout](https://eth-sepolia.blockscout.com/address/0x9075B7e735E44b8CD5b9487628c4D6291a197056)** — All transactions and contract interactions are publicly auditable here.

Source: [`src/main/resources/solidity/MerkleRootAnchor.sol`](src/main/resources/solidity/MerkleRootAnchor.sol)  
ABI: [`src/main/resources/abi/AnchorContract.json`](src/main/resources/abi/AnchorContract.json)

---

## Data Flow

```
Upload:   File → SHA-256 → S3 (WORM) → Leaf Hash → Merkle Tree append → DB persist
Anchor:   Merkle Root → Smart Contract → TX hash + block number → DB checkpoint
Verify:   Leaf hash → Merkle proof path → Recompute root → Compare with anchored root
```

**Leaf hash formula**: `SHA-256(fileIdentifier + storageVersionId + contentHash + uploadTimestamp)`

---

## Testing

```bash
./gradlew test
```

Test suites: `MerkleTreeTests`, `CryptoUtilsTests`, `BlockchainAnchoringServiceTests`, `GasEstimationUseCaseImplTests`

A Postman collection for manual/integration testing is included: [`Integrity_Shield_API.postman_collection.json`](Integrity_Shield_API.postman_collection.json)

---

## License

Apache 2.0

