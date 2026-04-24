'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { CheckCircle2, Download, Loader2, Search, ShieldAlert, XCircle } from 'lucide-react';
import { AuditLogTable } from './AuditLogTable';
import { useToast } from '@/hooks/use-toast';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ApiError, apiFetchJson, apiRoutes } from '@/lib/api';
import type {
  AllVersionsResponse,
  AnchorVerificationResponse,
  AnchoringResult,
  FileVersionDetail,
  FileVersionsResponse,
  MerkleProofResponse,
  MerkleRootResponse,
  VerificationResponse,
} from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

type AuditLogsToolProps = {
  refreshKey?: number;
};

type FullAuditCheckResult = {
  runAt: string;
  fileVersion: FileVersionDetail;
  proof: MerkleProofResponse;
  currentRoot: MerkleRootResponse;
  inclusion: VerificationResponse;
  rootConsistency: boolean;
  latestAnchor: AnchoringResult | null;
  anchorVerification: AnchorVerificationResponse | null;
  overall: 'verified' | 'needs-review';
  notes: string[];
  apiTranscript: Array<{
    step: string;
    endpoint: string;
    outcome: 'pass' | 'fail' | 'skipped';
    detail: string;
  }>;
};

export function AuditLogsTool({ refreshKey }: AuditLogsToolProps) {
  const [logs, setLogs] = useState<FileVersionDetail[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoadingLogs, setIsLoadingLogs] = useState(true);
  const [isRefreshingLogs, setIsRefreshingLogs] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [isVersionsModalOpen, setIsVersionsModalOpen] = useState(false);
  const [selectedVersions, setSelectedVersions] = useState<FileVersionsResponse | null>(null);
  const [isFetchingVersions, setIsFetchingVersions] = useState(false);
  const [isAuditCheckModalOpen, setIsAuditCheckModalOpen] = useState(false);
  const [isRunningAuditCheck, setIsRunningAuditCheck] = useState(false);
  const [selectedAuditLog, setSelectedAuditLog] = useState<FileVersionDetail | null>(null);
  const [auditCheckResult, setAuditCheckResult] = useState<FullAuditCheckResult | null>(null);
  const [auditCheckError, setAuditCheckError] = useState<string | null>(null);
  const { toast } = useToast();

  const buildVerificationUrl = (fileVersion: FileVersionDetail, knownRoot: string) => {
    const query = new URLSearchParams({
      fileIdentifier: fileVersion.fileIdentifier,
      storageVersionId: fileVersion.storageVersionId,
      contentHash: fileVersion.contentHash,
      uploadTimestamp: fileVersion.uploadTimestamp || '',
      knownRoot,
    });

    return `/?${query.toString()}#verification`;
  };

  const fetchLogs = async (showRefreshState = false) => {
    if (showRefreshState) {
      setIsRefreshingLogs(true);
    } else {
      setIsLoadingLogs(true);
    }

    try {
      const data = await apiFetchJson<AllVersionsResponse>(apiRoutes.auditLogs);
      setLogs(data.versions);
      setTotalCount(data.count);
    } catch (error) {
      toast({
        title: 'Error Fetching Data',
        description: (error as Error).message,
        variant: 'destructive',
      });
      setLogs([]);
      setTotalCount(0);
    } finally {
      setIsLoadingLogs(false);
      setIsRefreshingLogs(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  useEffect(() => {
    if (refreshKey === undefined) {
      return;
    }

    fetchLogs(true);
  }, [refreshKey]);

  const filteredLogs = useMemo(() => {
    if (!searchTerm) {
      return logs;
    }

    return logs.filter(log =>
      log.fileIdentifier.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [searchTerm, logs]);

  const handleViewVersions = async (fileIdentifier: string) => {
    setIsVersionsModalOpen(true);
    setIsFetchingVersions(true);
    setSelectedVersions(null);

    try {
      const response = await apiFetchJson<FileVersionsResponse>(apiRoutes.fileVersions(fileIdentifier));
      setSelectedVersions(response);
    } catch (error) {
      toast({
        title: 'Error Fetching File Versions',
        description: (error as Error).message,
        variant: 'destructive',
      });
    } finally {
      setIsFetchingVersions(false);
    }
  };

  const runFullAuditCheck = async (log: FileVersionDetail) => {
    if (!log.uploadTimestamp) {
      throw new Error('This record is missing uploadTimestamp, so inclusion verification cannot be executed.');
    }

    const [proof, currentRoot] = await Promise.all([
      apiFetchJson<MerkleProofResponse>(apiRoutes.merkleProof(log.id)),
      apiFetchJson<MerkleRootResponse>(apiRoutes.currentRoot),
    ]);

    const inclusion = await apiFetchJson<VerificationResponse>(apiRoutes.verifyInclusion, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        fileIdentifier: log.fileIdentifier,
        storageVersionId: log.storageVersionId,
        contentHash: log.contentHash,
        uploadTimestamp: log.uploadTimestamp,
        knownRoot: currentRoot.currentRoot,
      }),
    });

    let latestAnchor: AnchoringResult | null = null;
    let anchorVerification: AnchorVerificationResponse | null = null;

    try {
      latestAnchor = await apiFetchJson<AnchoringResult>(apiRoutes.latestAnchor);
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 404) {
        throw error;
      }
    }

    if (latestAnchor) {
      anchorVerification = await apiFetchJson<AnchorVerificationResponse>(
        apiRoutes.verifyAnchor(latestAnchor.rootHash, latestAnchor.transactionHash)
      );
    }

    const rootConsistency = proof.rootHash === currentRoot.currentRoot;
    const anchorValid = anchorVerification ? anchorVerification.isValid : false;
    const notes: string[] = [];
    const apiTranscript: FullAuditCheckResult['apiTranscript'] = [
      {
        step: 'Proof Retrieval',
        endpoint: apiRoutes.merkleProof(log.id),
        outcome: 'pass',
        detail: `Fetched ${proof.proofPath.length} proof step(s).`,
      },
      {
        step: 'Current Root Lookup',
        endpoint: apiRoutes.currentRoot,
        outcome: 'pass',
        detail: `Leaf count at check time: ${currentRoot.leafCount}.`,
      },
      {
        step: 'Inclusion Verification',
        endpoint: apiRoutes.verifyInclusion,
        outcome: inclusion.verified ? 'pass' : 'fail',
        detail: inclusion.verified ? 'Metadata verifies inclusion in the current tree.' : 'Endpoint returned verified=false.',
      },
      {
        step: 'Latest Anchor Lookup',
        endpoint: apiRoutes.latestAnchor,
        outcome: latestAnchor ? 'pass' : 'skipped',
        detail: latestAnchor ? `Checkpoint #${latestAnchor.checkpointId} on ${latestAnchor.networkName}.` : 'No anchor found (404).',
      },
      {
        step: 'On-chain Anchor Verification',
        endpoint: latestAnchor ? apiRoutes.verifyAnchor(latestAnchor.rootHash, latestAnchor.transactionHash) : '/api/v1/audit/anchors/verify',
        outcome: latestAnchor ? (anchorVerification?.isValid ? 'pass' : 'fail') : 'skipped',
        detail: latestAnchor ? (anchorVerification?.message || 'Anchor verification executed.') : 'Skipped because no latest anchor exists.',
      },
    ];

    if (!latestAnchor) {
      notes.push('No blockchain anchor exists yet, so on-chain validation could not be completed.');
    }

    if (latestAnchor && !anchorVerification?.isValid) {
      notes.push('Latest anchor verification failed for the current root/transaction pair.');
    }

    if (!rootConsistency) {
      notes.push('Proof root does not match the current root returned by the API.');
    }

    if (!inclusion.verified) {
      notes.push('Inclusion verification endpoint returned false for this metadata set.');
    }

    const overall = inclusion.verified && rootConsistency && anchorValid ? 'verified' : 'needs-review';

    return {
      runAt: new Date().toISOString(),
      fileVersion: log,
      proof,
      currentRoot,
      inclusion,
      rootConsistency,
      latestAnchor,
      anchorVerification,
      overall,
      notes,
      apiTranscript,
    } satisfies FullAuditCheckResult;
  };

  const handleRunAuditCheck = async (log: FileVersionDetail) => {
    setSelectedAuditLog(log);
    setAuditCheckResult(null);
    setAuditCheckError(null);
    setIsAuditCheckModalOpen(true);
    setIsRunningAuditCheck(true);

    try {
      const result = await runFullAuditCheck(log);
      setAuditCheckResult(result);
    } catch (error) {
      setAuditCheckError((error as Error).message);
    } finally {
      setIsRunningAuditCheck(false);
    }
  };

  const handleExportAuditEvidence = () => {
    if (!auditCheckResult) {
      return;
    }

    const payload = {
      schemaVersion: '1.0',
      generatedAt: new Date().toISOString(),
      auditCheck: auditCheckResult,
    };

    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = `audit-evidence-record-${auditCheckResult.fileVersion.id}.json`;
    link.click();
    URL.revokeObjectURL(objectUrl);
  };

  const handleCopyApiTranscript = async () => {
    if (!auditCheckResult) {
      return;
    }

    try {
      await navigator.clipboard.writeText(JSON.stringify(auditCheckResult.apiTranscript, null, 2));
      toast({
        title: 'API transcript copied',
        description: 'The full API step transcript is now in your clipboard.',
      });
    } catch {
      toast({
        title: 'Copy failed',
        description: 'Clipboard permissions are unavailable in this context.',
        variant: 'destructive',
      });
    }
  };

  const handleOpenVerificationTool = () => {
    if (!auditCheckResult?.fileVersion.uploadTimestamp) {
      return;
    }

    window.location.assign(buildVerificationUrl(auditCheckResult.fileVersion, auditCheckResult.currentRoot.currentRoot));
  };

  const statusBadge = (isPass: boolean, passLabel = 'Pass', failLabel = 'Fail') => (
    <Badge variant={isPass ? 'secondary' : 'destructive'}>{isPass ? passLabel : failLabel}</Badge>
  );

  return (
    <>
      <Card className="flex h-full flex-col">
        <CardHeader>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Audit Logs</CardTitle>
              <CardDescription>
                Review all stored file versions and inspect Merkle proof data from the audit service.
              </CardDescription>
            </div>
            <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-center">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchLogs(true)}
                disabled={isLoadingLogs || isRefreshingLogs}
              >
                {isRefreshingLogs ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Refresh
              </Button>
              <div className="relative ml-auto w-full sm:max-w-md">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="Filter by file identifier..."
                  className="w-full rounded-lg bg-background pl-8"
                  value={searchTerm}
                  onChange={event => setSearchTerm(event.target.value)}
                />
              </div>
            </div>
          </div>
        </CardHeader>
        <CardContent className="flex-grow">
          <div className="mb-4 text-xs text-muted-foreground">
            {isLoadingLogs ? 'Loading audit log records...' : `${filteredLogs.length} of ${totalCount} records shown.`}
          </div>
          <AuditLogTable
            logs={filteredLogs}
            onViewVersions={handleViewVersions}
            onRunAuditCheck={handleRunAuditCheck}
            isLoading={isLoadingLogs}
          />
        </CardContent>
      </Card>

      <Dialog open={isVersionsModalOpen} onOpenChange={setIsVersionsModalOpen}>
        <DialogContent className="sm:max-w-5xl">
          <DialogHeader>
            <DialogTitle>File Versions</DialogTitle>
            <DialogDescription>
              Complete version history for the selected file identifier.
            </DialogDescription>
          </DialogHeader>
          {isFetchingVersions ? (
            <div className="flex h-48 items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : selectedVersions ? (
            <div className="space-y-4">
              <div className="text-sm text-muted-foreground">
                {selectedVersions.count} version record(s) for <span className="font-medium text-foreground">{selectedVersions.fileIdentifier}</span>
              </div>
              <div className="space-y-2">
                {selectedVersions.versions.map(version => (
                  <div key={version.id} className="flex flex-col gap-3 rounded-lg border p-3">
                    <div className="space-y-1 text-sm">
                      <p className="font-medium">Record #{version.id}</p>
                      <p className="text-muted-foreground">Leaf Index: {version.leafIndex}</p>
                      {version.uploadTimestamp && (
                        <p className="select-all font-mono text-xs text-muted-foreground">{version.uploadTimestamp}</p>
                      )}
                      <p className="break-all font-mono text-xs text-muted-foreground select-all">{version.storageVersionId}</p>
                      <p className="break-all font-mono text-xs text-muted-foreground select-all">{version.contentHash}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Version history is unavailable.</p>
          )}
        </DialogContent>
      </Dialog>

      <Dialog open={isAuditCheckModalOpen} onOpenChange={setIsAuditCheckModalOpen}>
        <DialogContent className="flex max-h-[90vh] w-[calc(100vw-2rem)] max-w-5xl flex-col overflow-hidden p-0">
          <DialogHeader className="shrink-0 border-b px-6 py-4">
            <DialogTitle>Full Audit Check</DialogTitle>
            <DialogDescription>
              End-to-end integrity decision for record #{selectedAuditLog?.id ?? '-'} using current inclusion and blockchain anchor verification.
            </DialogDescription>
          </DialogHeader>

          <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-auto px-6 pb-6 pt-4">
            {isRunningAuditCheck ? (
              <div className="flex min-h-[16rem] items-center justify-center">
                <Loader2 className="mr-2 h-5 w-5 animate-spin text-muted-foreground" />
                <span className="text-sm text-muted-foreground">Running full audit checks...</span>
              </div>
            ) : auditCheckError ? (
              <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
                {auditCheckError}
              </div>
            ) : auditCheckResult ? (
              <>
                <div className="flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="text-sm font-medium">Overall Integrity Verdict</div>
                    <div className="text-xs text-muted-foreground">Run at {new Date(auditCheckResult.runAt).toLocaleString()}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    {auditCheckResult.overall === 'verified' ? <CheckCircle2 className="h-4 w-4 text-green-500" /> : <ShieldAlert className="h-4 w-4 text-amber-500" />}
                    <Badge variant={auditCheckResult.overall === 'verified' ? 'secondary' : 'destructive'}>
                      {auditCheckResult.overall === 'verified' ? 'Verified' : 'Needs Review'}
                    </Badge>
                  </div>
                </div>

                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-lg border p-3 text-sm">
                    <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Inclusion</div>
                    {statusBadge(auditCheckResult.inclusion.verified)}
                  </div>
                  <div className="rounded-lg border p-3 text-sm">
                    <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Proof Root vs Current Root</div>
                    {statusBadge(auditCheckResult.rootConsistency)}
                  </div>
                  <div className="rounded-lg border p-3 text-sm">
                    <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Latest Anchor Present</div>
                    {statusBadge(Boolean(auditCheckResult.latestAnchor), 'Present', 'Missing')}
                  </div>
                  <div className="rounded-lg border p-3 text-sm">
                    <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">On-chain Anchor Verification</div>
                    {statusBadge(Boolean(auditCheckResult.anchorVerification?.isValid), 'Valid', 'Invalid / N/A')}
                  </div>
                </div>

                {auditCheckResult.notes.length > 0 ? (
                  <div className="space-y-2 rounded-lg border p-4">
                    <div className="text-sm font-medium">Review Notes</div>
                    <ul className="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
                      {auditCheckResult.notes.map(note => (
                        <li key={note}>{note}</li>
                      ))}
                    </ul>
                  </div>
                ) : null}

                <div className="flex justify-end">
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="outline"
                      onClick={handleOpenVerificationTool}
                      disabled={!auditCheckResult.fileVersion.uploadTimestamp}
                    >
                      Open in Verification Tool
                    </Button>
                    <Button variant="outline" onClick={handleCopyApiTranscript}>
                      Copy API Transcript
                    </Button>
                    <Button variant="outline" onClick={handleExportAuditEvidence}>
                      <Download className="mr-2 h-4 w-4" />
                      Export Evidence JSON
                    </Button>
                  </div>
                </div>

                <details className="rounded-lg border border-border/70 bg-background/60 p-4">
                  <summary className="cursor-pointer text-sm font-medium">Evidence Details</summary>
                  <div className="mt-4 space-y-4">
                    <div>
                      <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Proof Payload</div>
                      <pre className="max-h-64 overflow-auto rounded-md border bg-muted/40 p-3 text-xs">
                        {JSON.stringify(auditCheckResult.proof, null, 2)}
                      </pre>
                    </div>
                    <div>
                      <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">API Transcript</div>
                      <pre className="max-h-64 overflow-auto rounded-md border bg-muted/40 p-3 text-xs">
                        {JSON.stringify(auditCheckResult.apiTranscript, null, 2)}
                      </pre>
                    </div>
                  </div>
                </details>
              </>
            ) : (
              <div className="flex min-h-[12rem] flex-col items-center justify-center text-center text-muted-foreground">
                <XCircle className="h-8 w-8" />
                <p className="mt-2 text-sm">No audit result available.</p>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}