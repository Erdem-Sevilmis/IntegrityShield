import type { SVGProps } from "react";

export function ShieldLockIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
      <rect width="6" height="8" x="9" y="9" rx="1" />
      <path d="M8 9a4 4 0 0 1 8 0" />
    </svg>
  );
}
