# Integrity-Shield UI

Next.js frontend for the Integrity-Shield dashboard.

## Local development

1. Install dependencies with `npm install`.
2. Create a `.env.local` file based on `.env.example`.
3. Set `API_BASE_URL` to your locally running backend, for example `http://localhost:8080`.
4. Start the UI with `npm run dev`.

The browser calls the API through a same-origin proxy path so local development does not depend on backend CORS headers.

## Backend integration

The UI expects a locally reachable API and uses a shared client in `src/lib/api.ts`.

- Rewrite target base URL: `API_BASE_URL`
- Browser proxy prefix: `NEXT_PUBLIC_API_PROXY_PREFIX` defaulting to `/backend`
- Current root: `/api/v1/audit/current-root`
- Audit logs: `/api/v1/audit/all-versions`
- File versions: `/api/v1/audit/versions/:fileIdentifier`
- Merkle proof: `/api/v1/audit/proof/:fileVersionId`
- Latest anchor: `/api/v1/audit/latest-anchor`
- Manual anchor: `/api/v1/audit/anchor-root`
- Gas estimate: `/api/v1/audit/gas-estimate`
- Upload: `/api/v1/files/upload`
- Verification: `/api/v1/audit/verify-inclusion`

If you change `next.config.ts`, restart the Next.js dev server so the rewrite table is rebuilt.