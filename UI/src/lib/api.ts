const DEFAULT_API_BASE_URL = 'http://localhost:8080';
const DEFAULT_API_PROXY_PREFIX = '/backend';

const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '');

export const apiBaseUrl = trimTrailingSlash(
    process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL
);

export const apiProxyPrefix = trimTrailingSlash(
    process.env.NEXT_PUBLIC_API_PROXY_PREFIX?.trim() || DEFAULT_API_PROXY_PREFIX
);

export const apiRoutes = {
    upload: '/api/v1/files/upload',
    currentRoot: '/api/v1/audit/current-root',
    verifyInclusion: '/api/v1/audit/verify-inclusion',
    merkleProof: (fileVersionId: number | string) => `/api/v1/audit/proof/${fileVersionId}`,
    auditLogs: '/api/v1/audit/all-versions',
    fileVersions: (fileIdentifier: string) => `/api/v1/audit/versions/${encodeURIComponent(fileIdentifier)}`,
    triggerAnchor: '/api/v1/audit/anchor-root',
    latestAnchor: '/api/v1/audit/latest-anchor',
    verifyAnchor: (rootHash: string, transactionHash: string) => `/api/v1/audit/anchors/verify?rootHash=${encodeURIComponent(rootHash)}&transactionHash=${encodeURIComponent(transactionHash)}`,
    gasEstimate: '/api/v1/audit/gas-estimate',
};

export class ApiError extends Error {
    status: number;
    code?: string;

    constructor(message: string, status: number, code?: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.code = code;
    }
}

type ResponseErrorDetails = {
    message: string;
    code?: string;
};

export function buildApiUrl(path: string) {
    if (path.startsWith('http://') || path.startsWith('https://')) {
        return path;
    }

    const normalizedPath = path.startsWith('/') ? path : `/${path}`;

    if (typeof window !== 'undefined') {
        return `${apiProxyPrefix}${normalizedPath}`;
    }

    return `${apiBaseUrl}${normalizedPath}`;
}

async function readResponseError(response: Response): Promise<ResponseErrorDetails> {
    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        const payload = await response.json().catch(() => null) as Record<string, unknown> | null;
        const code = typeof payload?.error === 'string' && payload.error.trim() ? payload.error : undefined;
        const message = payload?.message ?? payload?.error ?? payload?.detail;
        if (typeof message === 'string' && message.trim()) {
            return {
                message,
                code,
            };
        }
    }

    const text = await response.text().catch(() => '');
    if (text.trim()) {
        return {
            message: text,
        };
    }

    return {
        message: `Request failed with status ${response.status}`,
    };
}

export async function apiFetch(path: string, init?: RequestInit) {
    const response = await fetch(buildApiUrl(path), init);
    if (!response.ok) {
        const errorDetails = await readResponseError(response);
        throw new ApiError(errorDetails.message, response.status, errorDetails.code);
    }
    return response;
}

export async function apiFetchJson<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await apiFetch(path, init);
    return response.json() as Promise<T>;
}

export async function tryReadJson<T>(response: Response): Promise<T | null> {
    const contentType = response.headers.get('content-type') || '';
    if (!contentType.includes('application/json')) {
        return null;
    }

    return response.json().catch(() => null) as Promise<T | null>;
}