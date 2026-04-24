'use client';

import { useEffect, useState } from 'react';
import type { MerkleProofResponse } from '@/lib/types';
import { cn } from '@/lib/utils';
import { CheckCircle2, File, GitCommitVertical, Loader2, ShieldAlert, ShieldCheck } from 'lucide-react';

interface MerkleVisualizerProps {
    proof: MerkleProofResponse;
}

type StepDirection = 'LEFT' | 'RIGHT';

type ProofStepView = {
    level: number;
    currentHash: string;
    siblingHash: string;
    siblingPosition: StepDirection;
    parentHash: string;
};

const hashPreview = (hash: string) => `${hash.slice(0, 16)}...${hash.slice(-8)}`;

const bytesToHex = (bytes: Uint8Array) => Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');

const sha256Hex = async (bytes: Uint8Array) => {
    const digest = await crypto.subtle.digest('SHA-256', bytes);
    return bytesToHex(new Uint8Array(digest));
};

const sha256HexConcat = async (...parts: string[]) => {
    const encoded = new TextEncoder().encode(parts.join(''));
    return sha256Hex(encoded);
};

const deriveProofSteps = async (proof: MerkleProofResponse) => {
    const derivedSteps: ProofStepView[] = [];
    let currentHash = proof.leafHash;

    for (const [index, proofStep] of proof.proofPath.entries()) {
        const left = proofStep.position === 'LEFT' ? proofStep.siblingHash : currentHash;
        const right = proofStep.position === 'LEFT' ? currentHash : proofStep.siblingHash;
        const parentHash = await sha256HexConcat(left, right);

        derivedSteps.push({
            level: index + 1,
            currentHash,
            siblingHash: proofStep.siblingHash,
            siblingPosition: proofStep.position,
            parentHash,
        });

        currentHash = parentHash;
    }

    return {
        derivedSteps,
        computedRoot: currentHash,
    };
};

const HashCard = ({
    label,
    hash,
    tone = 'default',
}: {
    label: string;
    hash: string;
    tone?: 'default' | 'leaf' | 'root' | 'derived';
}) => (
    <div className="rounded-xl border bg-background/80 p-3">
        <div className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</div>
        <div className={cn(
            'mt-2 flex items-center gap-2 rounded-lg border px-3 py-2',
            tone === 'leaf' && 'border-green-500/50 bg-green-500/10',
            tone === 'root' && 'border-sky-500/50 bg-sky-500/10',
            tone === 'derived' && 'border-primary/50 bg-primary/10'
        )}>
            {tone === 'leaf' ? <File className="h-4 w-4 shrink-0 text-green-500" /> : null}
            {tone === 'root' ? <ShieldCheck className="h-4 w-4 shrink-0 text-sky-500" /> : null}
            {tone === 'derived' ? <GitCommitVertical className="h-4 w-4 shrink-0 text-primary" /> : null}
            <span className="truncate font-mono text-[11px] sm:text-xs">{hashPreview(hash)}</span>
        </div>
        <div className="mt-2 break-all font-mono text-[11px] text-muted-foreground">{hash}</div>
    </div>
);

const MetaCard = ({ label, value }: { label: string; value: string | number }) => (
    <div className="rounded-lg border bg-background/80 px-3 py-2">
        <div className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</div>
        <div className="mt-1 text-sm font-medium">{value}</div>
    </div>
);

export function MerkleVisualizer({ proof }: MerkleVisualizerProps) {
    const [steps, setSteps] = useState<ProofStepView[]>([]);
    const [computedRoot, setComputedRoot] = useState<string | null>(null);
    const [isDeriving, setIsDeriving] = useState(true);
    const [derivationError, setDerivationError] = useState<string | null>(null);

    useEffect(() => {
        let isCancelled = false;

        const run = async () => {
            setIsDeriving(true);
            setDerivationError(null);

            try {
                const derived = await deriveProofSteps(proof);
                if (isCancelled) {
                    return;
                }

                setSteps(derived.derivedSteps);
                setComputedRoot(derived.computedRoot);
            } catch (error) {
                if (isCancelled) {
                    return;
                }

                setSteps([]);
                setComputedRoot(null);
                setDerivationError((error as Error).message || 'Could not derive the proof path.');
            } finally {
                if (!isCancelled) {
                    setIsDeriving(false);
                }
            }
        };

        run();

        return () => {
            isCancelled = true;
        };
    }, [proof]);

    const isVerified = computedRoot === proof.rootHash;

    return (
        <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
            <div className="grid grid-cols-1 gap-3 lg:grid-cols-4">
                <MetaCard label="Leaf Index" value={proof.leafIndex} />
                <MetaCard label="Proof Steps" value={proof.proofPath.length} />
                <MetaCard label="Generated At" value={new Date(proof.generatedAt).toLocaleString()} />
                <div className="rounded-lg border bg-background/80 px-3 py-2">
                    <div className="text-[11px] uppercase tracking-wide text-muted-foreground">Current Root Check</div>
                    <div className="mt-1 flex items-center gap-2 text-sm font-medium">
                        {isDeriving ? <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" /> : isVerified ? <CheckCircle2 className="h-4 w-4 text-green-500" /> : <ShieldAlert className="h-4 w-4 text-amber-500" />}
                        <span>{isDeriving ? 'Computing current proof' : isVerified ? 'Matches current Merkle root' : 'Current root mismatch'}</span>
                    </div>
                </div>
            </div>

            <div className="rounded-lg border border-border/70 bg-background/60 px-3 py-2 text-xs text-muted-foreground">
                This proof is generated from the live tree state. Different file versions can share upper proof steps because their paths converge as they approach the current root.
            </div>

            <div className="h-full min-h-0 flex-1 overflow-auto rounded-xl border bg-muted/20">
                <div className="min-w-[52rem] p-4 sm:p-6">
                    <div className="mx-auto flex max-w-5xl flex-col gap-4">
                        <HashCard label="Selected Leaf" hash={proof.leafHash} tone="leaf" />

                        {isDeriving ? (
                            <div className="flex min-h-[18rem] items-center justify-center rounded-xl border border-dashed bg-background/60">
                                <div className="flex items-center gap-3 text-sm text-muted-foreground">
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    Deriving parent hashes from the proof path...
                                </div>
                            </div>
                        ) : derivationError ? (
                            <div className="rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-100">
                                {derivationError}
                            </div>
                        ) : (
                            <div className="flex flex-col gap-4">
                                {steps.map(step => (
                                    <div key={step.level} className="rounded-xl border bg-background/80 p-4 shadow-sm">
                                        <div className="flex flex-col gap-3 border-b pb-3 sm:flex-row sm:items-center sm:justify-between">
                                            <div>
                                                <div className="text-sm font-semibold">Proof Step {step.level}</div>
                                                <div className="text-xs text-muted-foreground">Combine the current hash with its sibling to derive the next parent.</div>
                                            </div>
                                            <div className={cn(
                                                'inline-flex w-fit items-center rounded-full border px-2.5 py-1 text-xs font-medium',
                                                step.siblingPosition === 'LEFT' ? 'border-sky-500/40 bg-sky-500/10 text-sky-200' : 'border-violet-500/40 bg-violet-500/10 text-violet-200'
                                            )}>
                                                Sibling on the {step.siblingPosition.toLowerCase()}
                                            </div>
                                        </div>

                                        <div className="mt-4 grid gap-4 lg:grid-cols-3">
                                            <HashCard label="Current Hash" hash={step.currentHash} />
                                            <HashCard label="Sibling Hash" hash={step.siblingHash} />
                                            <HashCard label="Derived Parent Hash" hash={step.parentHash} tone="derived" />
                                        </div>
                                    </div>
                                ))}

                                <HashCard label="Merkle Root" hash={proof.rootHash} tone="root" />
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
