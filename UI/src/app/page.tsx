'use client';

import { useState } from 'react';
import { DataRoom } from '@/components/dashboard/DataRoom';
import { AnchorStatus } from '@/components/dashboard/AnchorStatus';
import { AuditLogsTool } from '@/components/dashboard/AuditLogsTool';
import { CurrentRootCard } from '@/components/dashboard/CurrentRootCard';
import { GasEstimateCard } from '@/components/dashboard/GasEstimateCard';
import { VerificationTool } from '@/components/dashboard/VerificationTool';

export default function DashboardPage() {
  const [auditLogsRefreshKey, setAuditLogsRefreshKey] = useState(0);

  const handleUploadSuccess = () => {
    setAuditLogsRefreshKey(prev => prev + 1);
  };

  return (
    <div className="space-y-6">
      <section id="overview" className="scroll-mt-24">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          <CurrentRootCard />
          <AnchorStatus />
          <GasEstimateCard />
        </div>
      </section>

      <section id="upload" className="scroll-mt-24">
        <DataRoom onUploadSuccess={handleUploadSuccess} />
      </section>

      <section id="verification" className="scroll-mt-24">
        <VerificationTool />
      </section>

      <section id="audit-logs" className="scroll-mt-24">
        <AuditLogsTool refreshKey={auditLogsRefreshKey} />
      </section>
    </div>
  );
}
