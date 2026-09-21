export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fields: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function getErrorPayload(response: Response): Promise<Record<string, string>> {
  try {
    const payload: unknown = await response.json();

    if (typeof payload === 'object' && payload !== null) {
      return payload as Record<string, string>;
    }
  } catch {
    return {};
  }

  return {};
}

export async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const fields = await getErrorPayload(response);
    const message = fields.error ?? Object.values(fields)[0] ?? 'No fue posible completar la operacion.';

    throw new ApiError(message, response.status, fields);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
