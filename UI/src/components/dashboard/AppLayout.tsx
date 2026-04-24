'use client';

import { usePathname } from 'next/navigation';
import Link from 'next/link';
import Header from '@/components/dashboard/Header';
import { cn } from '@/lib/utils';

const navigationItems = [
  { href: '/#overview', label: 'Overview', paths: ['/'] },
  { href: '/#upload', label: 'Upload', paths: ['/'] },
  { href: '/#verification', label: 'Verification', paths: ['/'] },
  { href: '/#audit-logs', label: 'Audit Logs', paths: ['/'] },
];

export function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="min-h-screen bg-background">
      <Header>
        <div className="flex w-full flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-lg font-semibold">Integrity-Shield</h1>
            <p className="text-sm text-muted-foreground">Basic UI for upload, audit, and verification.</p>
          </div>
          <nav className="flex flex-wrap gap-2">
            {navigationItems.map(item => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'rounded-md px-3 py-2 text-sm transition-colors',
                  item.paths.includes(pathname)
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                )}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
      </Header>
      <main className="mx-auto w-full max-w-[1600px] p-4 sm:p-6 xl:px-8">
        {children}
      </main>
    </div>
  );
}
