import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface HeaderProps extends HTMLAttributes<HTMLDivElement> { }

export default function Header({ children, className }: HeaderProps) {
  return (
    <header className={cn('sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80', className)}>
      <div className="mx-auto flex w-full max-w-[1600px] items-center px-4 py-4 sm:px-6 xl:px-8">
        {children}
      </div>
    </header>
  );
}
