'use client';

import { useState, useMemo, useRef, type ChangeEvent, type DragEvent, type MouseEvent } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { FileUp, Lock, Loader2, FileCheck2, AlertTriangle, X } from 'lucide-react';
import { apiRoutes, buildApiUrl, tryReadJson } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import type { UploadResponse, VerificationRequest } from '@/lib/types';

type UploadStatus = 'idle' | 'hashing' | 'uploading' | 'success' | 'idempotent' | 'error';

type UploadErrorPayload = {
  error?: string;
  message?: string;
};

type DataRoomProps = {
  onUploadSuccess?: () => void;
};

export function DataRoom({ onUploadSuccess }: DataRoomProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [status, setStatus] = useState<UploadStatus>('idle');
  const [fileName, setFileName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();

  const buildVerificationPayload = (payload: UploadResponse): VerificationRequest => ({
    fileIdentifier: payload.fileIdentifier,
    storageVersionId: payload.storageVersionId,
    contentHash: payload.contentHash,
    uploadTimestamp: payload.uploadTimestamp,
    knownRoot: payload.currentRoot,
  });

  const buildVerificationUrl = (payload: VerificationRequest) => {
    const query = new URLSearchParams({
      fileIdentifier: payload.fileIdentifier,
      storageVersionId: payload.storageVersionId,
      contentHash: payload.contentHash,
      uploadTimestamp: payload.uploadTimestamp,
      knownRoot: payload.knownRoot,
    });

    return `/?${query.toString()}#verification`;
  };

  const copyVerificationPayload = async (payload: UploadResponse) => {
    const verificationPayload = buildVerificationPayload(payload);

    try {
      await navigator.clipboard.writeText(JSON.stringify(verificationPayload, null, 2));
      toast({
        title: 'Verification payload copied',
        description: 'You can paste this directly into inclusion verification workflows.',
      });
    } catch {
      toast({
        title: 'Copy failed',
        description: 'Clipboard permissions are unavailable in this context.',
        variant: 'destructive',
      });
    }
  };

  const openVerificationTool = (payload: UploadResponse) => {
    const verificationPayload = buildVerificationPayload(payload);

    window.dispatchEvent(
      new CustomEvent<VerificationRequest>('integrityshield:prefill-verification', {
        detail: verificationPayload,
      })
    );

    const verificationSection = document.getElementById('verification');
    if (verificationSection) {
      verificationSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }

    window.location.assign(buildVerificationUrl(verificationPayload));
  };

  const handleDragEnter = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    setError(null);

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
    e.target.value = '';
  };

  const handleFileUpload = async (file: File) => {
    setFileName(file.name);
    setStatus('hashing');
    setError(null);
    setUploadResult(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      setStatus('uploading');
      const response = await fetch(buildApiUrl(apiRoutes.upload), {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const payload = await tryReadJson<UploadErrorPayload>(response);
        const message = payload?.message || payload?.error || `Upload failed with status ${response.status}`;

        throw new Error(message);
      }

      const payload = await response.json() as UploadResponse;
      setUploadResult(payload);
      setStatus(response.status === 200 || payload.status === 'idempotent' ? 'idempotent' : 'success');
      onUploadSuccess?.();
    } catch (uploadError) {
      const message = (uploadError as Error).message;
      setError(message || 'Upload failed. Please try again.');
      setStatus('error');
    }
  };

  const reset = () => {
    setStatus('idle');
    setFileName(null);
    setError(null);
    setUploadResult(null);
  };

  const openFilePicker = () => {
    reset();
    fileInputRef.current?.click();
  };

  const handleUploadAnotherFileClick = (e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    openFilePicker();
  };

  const handleCopyVerificationPayloadClick = async (e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    if (!uploadResult) {
      return;
    }

    await copyVerificationPayload(uploadResult);
  };

  const handleOpenVerificationToolClick = (e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    if (!uploadResult) {
      return;
    }

    openVerificationTool(uploadResult);
  };

  const handleDropzoneClick = (e: MouseEvent<HTMLDivElement>) => {
    if (status !== 'success' && status !== 'idempotent') {
      return;
    }

    const target = e.target;
    if (!(target instanceof Element)) {
      return;
    }

    if (target.closest('[data-preserve-upload-result="true"]')) {
      return;
    }

    reset();
  };

  const statusContent = useMemo(() => {
    switch (status) {
      case 'idle':
        return (
          <>
            <FileUp className="mx-auto h-12 w-12 text-muted-foreground" />
            <p className="mt-4 text-center text-muted-foreground">
              Drag & drop a file here or{' '}
              <label htmlFor="file-upload" className="font-medium text-primary underline-offset-4 hover:underline cursor-pointer">
                choose a file
              </label>
            </p>
            <p className="mt-2 text-center text-xs text-muted-foreground">
              Upload starts immediately after you drop or choose a file.
            </p>
          </>
        );
      case 'hashing':
        return (
          <div className="flex flex-col items-center justify-center text-center">
            <Loader2 className="h-12 w-12 animate-spin text-primary" />
            <p className="mt-4 font-medium">Calculating SHA-256 hash...</p>
            <p className="text-sm text-muted-foreground truncate max-w-full px-4">{fileName}</p>
          </div>
        );
      case 'uploading':
        return (
          <div className="flex flex-col items-center justify-center text-center">
            <Loader2 className="h-12 w-12 animate-spin text-primary" />
            <p className="mt-4 font-medium">Securing to immutable storage...</p>
            <p className="text-sm text-muted-foreground truncate max-w-full px-4">{fileName}</p>
          </div>
        );
      case 'success':
        return (
          <div className="flex w-full max-w-2xl flex-col items-center justify-center text-center">
            <FileCheck2 className="h-12 w-12 text-green-500" />
            <p className="mt-4 font-medium">File Uploaded</p>
            <p className="text-sm text-muted-foreground truncate max-w-full px-4">{uploadResult?.message || fileName}</p>
            {uploadResult && (
              <div className="mt-3 w-full rounded-md bg-muted p-3 text-left text-xs text-muted-foreground">
                <p>recordId: {uploadResult.id}</p>
                <p>storageVersionId: {uploadResult.storageVersionId}</p>
                <p>leafIndex: {uploadResult.leafIndex}</p>
                <p>uploadTimestamp: {uploadResult.uploadTimestamp}</p>
              </div>
            )}
            {uploadResult ? (
              <div className="mt-4 flex flex-wrap justify-center gap-2" data-preserve-upload-result="true">
                <Button variant="outline" size="sm" onClick={handleCopyVerificationPayloadClick} data-preserve-upload-result="true">
                  Copy Verification Payload
                </Button>
                <Button variant="outline" size="sm" onClick={handleOpenVerificationToolClick} data-preserve-upload-result="true">
                  Open in Verification Tool
                </Button>
              </div>
            ) : null}
            <Button onClick={handleUploadAnotherFileClick} variant="outline" size="sm" className="mt-4">Upload Another File</Button>
          </div>
        );
      case 'idempotent':
        return (
          <div className="flex w-full max-w-2xl flex-col items-center justify-center text-center">
            <AlertTriangle className="h-12 w-12 text-yellow-500" />
            <p className="mt-4 font-medium">Idempotent Upload</p>
            <p className="mt-2 text-sm text-muted-foreground max-w-full px-4">{uploadResult?.message || 'Identical content already exists. No new leaf was created.'}</p>
            {uploadResult && (
              <div className="mt-2 w-full rounded-md bg-muted p-3 text-left text-xs text-muted-foreground">
                <p>fileIdentifier: {uploadResult.fileIdentifier}</p>
                <p>storageVersionId: {uploadResult.storageVersionId}</p>
                <p>leafIndex: {uploadResult.leafIndex}</p>
                <p>currentRoot: {uploadResult.currentRoot}</p>
              </div>
            )}
            {uploadResult ? (
              <div className="mt-4 flex flex-wrap justify-center gap-2" data-preserve-upload-result="true">
                <Button variant="outline" size="sm" onClick={handleCopyVerificationPayloadClick} data-preserve-upload-result="true">
                  Copy Verification Payload
                </Button>
                <Button variant="outline" size="sm" onClick={handleOpenVerificationToolClick} data-preserve-upload-result="true">
                  Open in Verification Tool
                </Button>
              </div>
            ) : null}
            <Button onClick={handleUploadAnotherFileClick} variant="outline" size="sm" className="mt-4">Upload Another File</Button>
          </div>
        );
      case 'error':
        return (
          <div className="flex w-full max-w-2xl flex-col items-center justify-center text-center">
            <AlertTriangle className="h-12 w-12 text-destructive" />
            <p className="mt-4 font-medium">Upload Failed</p>
            <p className="mt-2 text-sm text-muted-foreground max-w-full px-4">{error}</p>
            <Button onClick={reset} variant="outline" size="sm" className="mt-4">Try Again</Button>
          </div>
        )
      default:
        return null;
    }
  }, [status, fileName, error, uploadResult]);

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <CardTitle>Data Room</CardTitle>
            <div className="flex items-center gap-1.5 rounded-full bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
              <Lock className="h-3 w-3" />
              <span>WORM Storage</span>
            </div>
          </div>
          {status !== 'idle' && (
            <Button variant="ghost" size="icon" className="h-6 w-6" onClick={reset}>
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
        <CardDescription>Securely upload files for immutable logging and auditing.</CardDescription>
      </CardHeader>
      <CardContent>
        <div
          className={cn(
            'flex min-h-64 w-full flex-col items-center justify-center rounded-lg border-2 border-dashed bg-muted/50 p-6 transition-colors',
            isDragging && 'border-primary bg-primary/10',
            (status === 'success' || status === 'idempotent' || status === 'error') && 'min-h-[22rem]',
            (status === 'success' || status === 'idempotent' || status === 'error') && 'border-solid'
          )}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={handleDropzoneClick}
        >
          {statusContent}
        </div>
        <input
          ref={fileInputRef}
          id="file-upload"
          name="file-upload"
          type="file"
          className="sr-only"
          onChange={handleFileSelect}
        />
      </CardContent>
    </Card>
  );
}
