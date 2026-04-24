'use client';
import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import type { AnchoringResult } from '@/lib/types';
import { AlertTriangle, ExternalLink, CheckCircle2, Loader2, Zap } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useToast } from '@/hooks/use-toast';
import { ApiError, apiFetchJson, apiRoutes } from '@/lib/api';

const BLOCKSCOUT_SEPOLIA_TX_BASE_URL = 'https://eth-sepolia.blockscout.com/tx';

const ClientFormattedDate = ({ timestamp }: { timestamp: string }) => {
  const [formattedDate, setFormattedDate] = useState('');

  useEffect(() => {
    setFormattedDate(formatDistanceToNow(new Date(timestamp), { addSuffix: true }));
  }, [timestamp]);

  if (!formattedDate) {
    return null;
  }

  return <>{formattedDate}</>;
};

export function AnchorStatus() {
  const [data, setData] = useState<AnchoringResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAnchoring, setIsAnchoring] = useState(false);
  const [anchorNotice, setAnchorNotice] = useState<string | null>(null);
  const { toast } = useToast();

  const fetchStatus = useCallback(async () => {
    try {
      const statusData = await apiFetchJson<AnchoringResult>(apiRoutes.latestAnchor);
      setData(statusData);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setData(null);
        return;
      }

      toast({
        title: 'Error Fetching Status',
        description: (error as Error).message,
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    setIsLoading(true);
    fetchStatus();
  }, [fetchStatus]);

  const handleManualAnchor = async () => {
    setIsAnchoring(true);
    setAnchorNotice(null);
    try {
      const response = await apiFetchJson<AnchoringResult>(apiRoutes.triggerAnchor, { method: 'POST' });
      setData(response);
      toast({
        title: 'Root anchored',
        description: `Checkpoint #${response.checkpointId} anchored on ${response.networkName}.`,
      });
    } catch (error) {
      if (error instanceof ApiError && error.code === 'root_already_anchored') {
        setAnchorNotice(error.message);
        await fetchStatus();
        return;
      }

      toast({
        title: 'Error during anchoring',
        description: (error as Error).message,
        variant: 'destructive',
      });
      fetchStatus();
    } finally {
      setIsAnchoring(false);
    }
  }

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Blockchain Anchor Status</CardTitle>
          <CardDescription>Current Merkle root anchoring status.</CardDescription>
        </CardHeader>
        <CardContent className="h-[218px] flex items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle>Latest Anchor</CardTitle>
            <CardDescription>Most recent blockchain anchoring result.</CardDescription>
          </div>
          {data ? (
            <Badge variant="secondary" className="bg-green-500/10 text-green-400 border-green-500/20"><CheckCircle2 className="mr-1 h-3 w-3" />Anchored</Badge>
          ) : (
            <Badge variant="outline">Not Anchored Yet</Badge>
          )}
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {anchorNotice ? (
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>Already Anchored</AlertTitle>
              <AlertDescription>{anchorNotice}</AlertDescription>
            </Alert>
          ) : null}
          {data ? (
            <div className="text-sm space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Anchored</span>
                <span><ClientFormattedDate timestamp={data.anchoredAt} /></span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Network</span>
                <span>{data.networkName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Checkpoint</span>
                <span>{data.checkpointId}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Block Number</span>
                <span>{data.blockNumber}</span>
              </div>
              <div className="space-y-1">
                <span className="text-muted-foreground">Root Hash</span>
                <p className="break-all font-mono text-xs text-muted-foreground">{data.rootHash}</p>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Transaction</span>
                <a
                  href={`${BLOCKSCOUT_SEPOLIA_TX_BASE_URL}/${data.transactionHash}`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1 text-primary hover:underline"
                >
                  <span>View on Blockscout</span>
                  <ExternalLink className="h-3 w-3" />
                </a>
              </div>
            </div>
          ) : (
            <div className="rounded-lg bg-muted/40 p-3 text-sm text-muted-foreground">
              No blockchain anchor has been retrieved yet.
            </div>
          )}
          <Button className="w-full" onClick={handleManualAnchor} disabled={isAnchoring}>
            {isAnchoring ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Zap className="mr-2 h-4 w-4" />}
            {isAnchoring ? 'Anchoring...' : 'Anchor Current Root'}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
