'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiFetchJson, apiRoutes } from '@/lib/api';
import type { MerkleRootResponse } from '@/lib/types';
import { Loader2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export function CurrentRootCard() {
    const [data, setData] = useState<MerkleRootResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const { toast } = useToast();

    const fetchCurrentRoot = async (showRefreshState = false) => {
        if (showRefreshState) {
            setIsRefreshing(true);
        } else {
            setIsLoading(true);
        }

        try {
            const response = await apiFetchJson<MerkleRootResponse>(apiRoutes.currentRoot);
            setData(response);
        } catch (error) {
            toast({
                title: 'Error Fetching Root',
                description: (error as Error).message,
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
            setIsRefreshing(false);
        }
    };

    useEffect(() => {
        fetchCurrentRoot();
    }, []);

    return (
        <Card>
            <CardHeader>
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <CardTitle>Current Merkle Root</CardTitle>
                        <CardDescription>Live tree state.</CardDescription>
                    </div>
                    <Button variant="outline" size="sm" onClick={() => fetchCurrentRoot(true)} disabled={isLoading || isRefreshing}>
                        {isRefreshing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                        Refresh
                    </Button>
                </div>
            </CardHeader>
            <CardContent>
                {isLoading && !data ? (
                    <div className="flex h-28 items-center justify-center">
                        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                    </div>
                ) : data ? (
                    <div className="space-y-4">
                        <div className="space-y-2 rounded-lg bg-muted/50 p-3">
                            <div>
                                <p className="text-xs uppercase tracking-wide text-muted-foreground">currentRoot</p>
                                <p className="mt-1 break-all font-mono text-xs text-muted-foreground">{data.currentRoot}</p>
                            </div>
                            <div className="flex items-center justify-between border-t pt-3">
                                <span className="text-sm text-muted-foreground">leafCount</span>
                                <Badge variant="secondary">{data.leafCount}</Badge>
                            </div>
                        </div>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground">Current root is unavailable.</p>
                )}
            </CardContent>
        </Card>
    );
}