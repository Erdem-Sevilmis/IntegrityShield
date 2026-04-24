'use client';
import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { CheckCircle2, Loader2, ShieldCheck, ShieldEllipsis, XCircle } from 'lucide-react';
import { ApiError, apiFetchJson, apiRoutes } from '@/lib/api';
import type {
  AnchorVerificationResponse,
  AnchoringResult,
  MerkleRootResponse,
  VerificationRequest,
  VerificationResponse,
} from '@/lib/types';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useSearchParams } from 'next/navigation';


type VerificationStatus = 'idle' | 'verifying' | 'success' | 'failed';

export function VerificationTool() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<VerificationStatus>('idle');
  const [anchorStatus, setAnchorStatus] = useState<VerificationStatus>('idle');
  const [formValues, setFormValues] = useState<VerificationRequest>({
    fileIdentifier: '',
    storageVersionId: '',
    contentHash: '',
    uploadTimestamp: '',
    knownRoot: '',
  });
  const [anchorValues, setAnchorValues] = useState({ rootHash: '', transactionHash: '' });
  const [result, setResult] = useState<VerificationResponse | null>(null);
  const [anchorResult, setAnchorResult] = useState<AnchorVerificationResponse | null>(null);
  const [resultMessage, setResultMessage] = useState<string | null>(null);
  const [anchorMessage, setAnchorMessage] = useState<string | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    const prefill = {
      fileIdentifier: searchParams.get('fileIdentifier') || '',
      storageVersionId: searchParams.get('storageVersionId') || '',
      contentHash: searchParams.get('contentHash') || '',
      uploadTimestamp: searchParams.get('uploadTimestamp') || '',
      knownRoot: searchParams.get('knownRoot') || '',
    };

    const hasPrefill = Object.values(prefill).some(Boolean);
    if (!hasPrefill) {
      return;
    }

    setFormValues(prev => ({
      ...prev,
      ...prefill,
    }));
  }, [searchParams]);

  useEffect(() => {
    const handlePrefillEvent = (event: Event) => {
      const customEvent = event as CustomEvent<Partial<VerificationRequest>>;
      const prefill = customEvent.detail;

      if (!prefill) {
        return;
      }

      const hasPrefill = Object.values(prefill).some(Boolean);
      if (!hasPrefill) {
        return;
      }

      setFormValues(prev => ({
        ...prev,
        ...prefill,
      }));
    };

    window.addEventListener('integrityshield:prefill-verification', handlePrefillEvent);

    return () => {
      window.removeEventListener('integrityshield:prefill-verification', handlePrefillEvent);
    };
  }, []);

  const handleVerify = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setStatus('verifying');
    setResultMessage(null);
    setResult(null);

    try {
      const response = await apiFetchJson<VerificationResponse>(apiRoutes.verifyInclusion, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formValues),
      });

      setResult(response);
      setStatus(response.verified ? 'success' : 'failed');
    } catch (error) {
      setStatus('failed');
      setResultMessage((error as Error).message);
    }
  };

  const handleUseCurrentRoot = async () => {
    try {
      const response = await apiFetchJson<MerkleRootResponse>(apiRoutes.currentRoot);
      setFormValues(prev => ({ ...prev, knownRoot: response.currentRoot }));
    } catch (error) {
      toast({
        title: 'Root lookup failed',
        description: (error as Error).message,
        variant: 'destructive',
      });
    }
  };

  const handleUseLatestAnchor = async () => {
    try {
      const response = await apiFetchJson<AnchoringResult>(apiRoutes.latestAnchor);
      setAnchorValues({ rootHash: response.rootHash, transactionHash: response.transactionHash });
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        toast({
          title: 'No anchor yet',
          description: 'The current Merkle root has not been anchored to the blockchain yet.',
        });
        return;
      }

      toast({
        title: 'Latest anchor lookup failed',
        description: (error as Error).message,
        variant: 'destructive',
      });
    }
  };

  const handleVerifyAnchor = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setAnchorStatus('verifying');
    setAnchorResult(null);
    setAnchorMessage(null);

    try {
      const response = await apiFetchJson<AnchorVerificationResponse>(
        apiRoutes.verifyAnchor(anchorValues.rootHash, anchorValues.transactionHash)
      );
      setAnchorResult(response);
      setAnchorMessage(response.message);
      setAnchorStatus(response.isValid ? 'success' : 'failed');
    } catch (error) {
      setAnchorStatus('failed');
      setAnchorMessage((error as Error).message);
    }
  };

  const VerificationResult = () => {
    if (status === 'success') {
      return (
        <Alert variant="default" className="mt-4 bg-green-500/10 border-green-500/20 text-green-300">
          <CheckCircle2 className="h-4 w-4 !text-green-400" />
          <AlertTitle>Verification Successful</AlertTitle>
          <AlertDescription>
            Verified file identifier <span className="font-medium">{result?.fileIdentifier}</span> using storage version <span className="font-medium">{result?.storageVersionId}</span>.
          </AlertDescription>
        </Alert>
      )
    }
    if (status === 'failed') {
      return (
        <Alert variant="destructive" className="mt-4">
          <XCircle className="h-4 w-4" />
          <AlertTitle>Verification Failed</AlertTitle>
          <AlertDescription>
            {resultMessage || "The provided metadata did not pass inclusion verification."}
          </AlertDescription>
        </Alert>
      )
    }
    return null;
  };

  const AnchorVerificationResult = () => {
    if (anchorStatus === 'success') {
      return (
        <Alert variant="default" className="mt-4 bg-green-500/10 border-green-500/20 text-green-300">
          <CheckCircle2 className="h-4 w-4 !text-green-400" />
          <AlertTitle>Anchor Verified</AlertTitle>
          <AlertDescription>{anchorMessage || 'The blockchain anchor is valid.'}</AlertDescription>
        </Alert>
      );
    }

    if (anchorStatus === 'failed') {
      return (
        <Alert variant="destructive" className="mt-4">
          <XCircle className="h-4 w-4" />
          <AlertTitle>Anchor Verification Failed</AlertTitle>
          <AlertDescription>{anchorMessage || 'The blockchain anchor could not be verified.'}</AlertDescription>
        </Alert>
      );
    }

    return null;
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Auditor Verification Tool</CardTitle>
        <CardDescription>
          Validate file inclusion proofs and verify anchored blockchain checkpoints.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="file-inclusion" className="space-y-4">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="file-inclusion">File Inclusion</TabsTrigger>
            <TabsTrigger value="anchor-verification">Anchor Verification</TabsTrigger>
          </TabsList>

          <TabsContent value="file-inclusion">
            <form onSubmit={handleVerify} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="fileIdentifier">File Identifier</Label>
                  <Input id="fileIdentifier" placeholder="document.pdf" required value={formValues.fileIdentifier} onChange={e => setFormValues(prev => ({ ...prev, fileIdentifier: e.target.value }))} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="storageVersionId">Storage Version ID</Label>
                  <Input id="storageVersionId" placeholder="v1-abc123def456" required value={formValues.storageVersionId} onChange={e => setFormValues(prev => ({ ...prev, storageVersionId: e.target.value }))} />
                </div>
              </div>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="contentHash">Content Hash</Label>
                  <Input id="contentHash" placeholder="64-char SHA-256 hex" required value={formValues.contentHash} onChange={e => setFormValues(prev => ({ ...prev, contentHash: e.target.value }))} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="uploadTimestamp">Upload Timestamp</Label>
                  <Input id="uploadTimestamp" placeholder="2026-03-05T14:23:45.123Z" required value={formValues.uploadTimestamp} onChange={e => setFormValues(prev => ({ ...prev, uploadTimestamp: e.target.value }))} />
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="knownRoot">Known Root</Label>
                  <Button type="button" variant="outline" size="sm" onClick={handleUseCurrentRoot}>
                    Use Current Root
                  </Button>
                </div>
                <Input id="knownRoot" placeholder="64-char SHA-256 hex" required value={formValues.knownRoot} onChange={e => setFormValues(prev => ({ ...prev, knownRoot: e.target.value }))} />
              </div>

              <VerificationResult />

              <Button type="submit" className="w-full sm:w-auto" disabled={status === 'verifying'}>
                {status === 'verifying' ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <ShieldCheck className="mr-2 h-4 w-4" />}
                {status === 'verifying' ? 'Verifying...' : 'Verify Inclusion'}
              </Button>
            </form>
          </TabsContent>

          <TabsContent value="anchor-verification">
            <form onSubmit={handleVerifyAnchor} className="space-y-4">
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="rootHash">Root Hash</Label>
                  <Button type="button" variant="outline" size="sm" onClick={handleUseLatestAnchor}>
                    Use Latest Anchor
                  </Button>
                </div>
                <Input id="rootHash" placeholder="64-char SHA-256 hex" required value={anchorValues.rootHash} onChange={e => setAnchorValues(prev => ({ ...prev, rootHash: e.target.value }))} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="transactionHash">Transaction Hash</Label>
                <Input id="transactionHash" placeholder="0x..." required value={anchorValues.transactionHash} onChange={e => setAnchorValues(prev => ({ ...prev, transactionHash: e.target.value }))} />
              </div>

              <AnchorVerificationResult />

              <Button type="submit" className="w-full sm:w-auto" disabled={anchorStatus === 'verifying'}>
                {anchorStatus === 'verifying' ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <ShieldEllipsis className="mr-2 h-4 w-4" />}
                {anchorStatus === 'verifying' ? 'Verifying...' : 'Verify Anchor'}
              </Button>
            </form>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
