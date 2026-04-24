'use client';

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import type { FileVersionDetail } from '@/lib/types';
import { Files, ShieldCheck } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';

type AuditLogTableProps = {
  logs: FileVersionDetail[];
  onViewVersions: (fileIdentifier: string) => void;
  onRunAuditCheck: (log: FileVersionDetail) => void;
  isLoading?: boolean;
};

export function AuditLogTable({ logs, onViewVersions, onRunAuditCheck, isLoading = false }: AuditLogTableProps) {
  return (
    <div className="rounded-md border overflow-x-auto">
      <Table className="min-w-[1200px]">
        <TableHeader>
          <TableRow>
            <TableHead className="min-w-[180px]">File Identifier</TableHead>
            <TableHead className="w-[90px]">Record ID</TableHead>
            <TableHead className="w-[100px]">Leaf Index</TableHead>
            <TableHead className="w-[240px]">Uploaded At</TableHead>
            <TableHead className="min-w-[260px]">Storage Version</TableHead>
            <TableHead className="min-w-[360px]">Content Hash</TableHead>
            <TableHead className="text-right w-[220px]">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            Array.from({ length: 5 }).map((_, i) => (
              <TableRow key={`skeleton-${i}`} data-state="loading">
                <TableCell><Skeleton className="h-5 w-32" /></TableCell>
                <TableCell><Skeleton className="h-5 w-12" /></TableCell>
                <TableCell><Skeleton className="h-5 w-12" /></TableCell>
                <TableCell><Skeleton className="h-5 w-40" /></TableCell>
                <TableCell><Skeleton className="h-5 w-20" /></TableCell>
                <TableCell><Skeleton className="h-5 w-48" /></TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="icon" disabled>
                    <ShieldCheck className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))
          ) : logs.length > 0 ? (
            logs.map(log => (
              <TableRow key={log.id} className="align-top">
                <TableCell className="font-medium break-all">{log.fileIdentifier}</TableCell>
                <TableCell className="align-top">{log.id}</TableCell>
                <TableCell className="align-top">{log.leafIndex}</TableCell>
                <TableCell className="align-top">
                  {log.uploadTimestamp ? (
                    <span className="select-all font-mono text-xs" title={log.uploadTimestamp}>
                      {log.uploadTimestamp}
                    </span>
                  ) : '-'}
                </TableCell>
                <TableCell className="font-mono text-xs break-all select-all align-top">{log.storageVersionId}</TableCell>
                <TableCell className="font-mono text-xs break-all select-all align-top">{log.contentHash}</TableCell>
                <TableCell className="text-right align-top">
                  <div className="flex items-center justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => onRunAuditCheck(log)} title="Run Full Audit Check">
                      <ShieldCheck className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => onViewVersions(log.fileIdentifier)} title="View File Versions">
                      <Files className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell colSpan={7} className="h-24 text-center">
                No audit logs found.
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}
