'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { apiFetchJson, apiRoutes } from '@/lib/api';
import type { GasEstimateResponse } from '@/lib/types';
import { Loader2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { format } from 'date-fns';

const formatMaybeCurrency = (value?: number | null, fallback = 'n/a') =>
    value === null || value === undefined ? fallback : value.toFixed(2);

export function GasEstimateCard() {
    const [data, setData] = useState<GasEstimateResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const { toast } = useToast();

    const fetchEstimate = async (showRefreshState = false) => {
        if (showRefreshState) {
            setIsRefreshing(true);
        } else {
            setIsLoading(true);
        }

        try {
            const response = await apiFetchJson<GasEstimateResponse>(apiRoutes.gasEstimate);
            setData(response);
        } catch (error) {
            toast({
                title: 'Error Fetching Gas Estimate',
                description: (error as Error).message,
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
            setIsRefreshing(false);
        }
    };

    useEffect(() => {
        fetchEstimate();
    }, []);

    return (
        <Card>
            <CardHeader>
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <CardTitle>Gas Estimate</CardTitle>
                        <CardDescription>Estimated blockchain cost.</CardDescription>
                    </div>
                    <Button variant="outline" size="sm" onClick={() => fetchEstimate(true)} disabled={isLoading || isRefreshing}>
                        {isRefreshing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                        Refresh
                    </Button>
                </div>
            </CardHeader>
            <CardContent>
                {isLoading && !data ? (
                    <div className="flex h-32 items-center justify-center">
                        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                    </div>
                ) : data ? (
                    <div className="space-y-4 text-sm">
                        <div className="rounded-lg bg-muted/50 p-3">
                            <div className="mb-2 font-medium">Testnet: {data.testnet.networkName}</div>
                            <div className="space-y-1 text-muted-foreground">
                                <div className="flex justify-between gap-4"><span>Gas Used</span><span>{data.testnet.gasUsed}</span></div>
                                <div className="flex justify-between gap-4"><span>Total Cost</span><span>{data.testnet.totalCostEth} ETH</span></div>
                                <div className="flex justify-between gap-4"><span>USD</span><span>{formatMaybeCurrency(data.testnet.totalCostUsd)}</span></div>
                                <div className="flex justify-between gap-4"><span>EUR</span><span>{formatMaybeCurrency(data.testnet.totalCostEur)}</span></div>
                            </div>
                        </div>
                        <Separator />
                        <div className="space-y-1 text-muted-foreground">
                            <div className="flex justify-between gap-4"><span>Mainnet Cost</span><span>{data.mainnetEstimate.estimatedCostEth} ETH</span></div>
                            <div className="flex justify-between gap-4"><span>Mainnet USD</span><span>{formatMaybeCurrency(data.mainnetEstimate.estimatedCostUsd)}</span></div>
                            <div className="flex justify-between gap-4"><span>Mainnet EUR</span><span>{formatMaybeCurrency(data.mainnetEstimate.estimatedCostEur)}</span></div>
                            <div className="flex justify-between gap-4"><span>Estimated At</span><span>{format(new Date(data.estimatedAt), 'PPp')}</span></div>
                        </div>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground">Gas estimate is unavailable.</p>
                )}
            </CardContent>
        </Card>
    );
}