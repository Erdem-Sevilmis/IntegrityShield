export type Sha256Hex = string;
export type TransactionHashHex = string;

export type MerkleRootResponse = {
  currentRoot: Sha256Hex;
  leafCount: number;
};

export type UploadResponse = {
  id: number;
  fileIdentifier: string;
  storageVersionId: string;
  contentHash: Sha256Hex;
  leafIndex: number;
  uploadTimestamp: string;
  currentRoot: Sha256Hex;
  leafCount: number;
  message: string;
  status: string | null;
};

export type VerificationRequest = {
  fileIdentifier: string;
  storageVersionId: string;
  contentHash: Sha256Hex;
  uploadTimestamp: string;
  knownRoot: Sha256Hex;
};

export type VerificationResponse = {
  verified: boolean;
  fileIdentifier: string;
  storageVersionId: string;
  contentHash: Sha256Hex;
};

export type ProofStep = {
  siblingHash: Sha256Hex;
  position: 'LEFT' | 'RIGHT';
};

export type MerkleProofResponse = {
  leafHash: Sha256Hex;
  leafIndex: number;
  proofPath: ProofStep[];
  rootHash: Sha256Hex;
  generatedAt: string;
};

export type FileVersionSummary = {
  id: number;
  fileIdentifier: string;
  storageVersionId: string;
  contentHash: Sha256Hex;
  leafIndex: number;
};

export type FileVersionDetail = FileVersionSummary & {
  uploadTimestamp?: string;
};

export type AllVersionsResponse = {
  count: number;
  versions: FileVersionDetail[];
};

export type FileVersionsResponse = {
  fileIdentifier: string;
  count: number;
  versions: FileVersionDetail[];
};

export type AnchoringResult = {
  checkpointId: number;
  rootHash: Sha256Hex;
  transactionHash: TransactionHashHex;
  blockNumber: number;
  anchoredAt: string;
  networkName: string;
};

export type AnchorVerificationResponse = {
  rootHash: Sha256Hex;
  transactionHash: TransactionHashHex;
  isValid: boolean;
  message: string;
};

export type TestnetEstimate = {
  networkName: string;
  gasPriceWei: number;
  gasUsed: number;
  totalCostWei: number;
  totalCostEth: number;
  totalCostUsd?: number | null;
  totalCostEur?: number | null;
};

export type MainnetEstimate = {
  gasPriceWei: number;
  gasUsed: number;
  estimatedCostWei: number;
  estimatedCostEth: number;
  estimatedCostUsd?: number | null;
  estimatedCostEur?: number | null;
};

export type GasEstimateResponse = {
  testnet: TestnetEstimate;
  mainnetEstimate: MainnetEstimate;
  ethToUsdRate?: number | null;
  ethToEurRate?: number | null;
  estimatedAt: string;
};
