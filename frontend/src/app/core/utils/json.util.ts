export function parseJsonSafe<T>(raw: string | null | undefined): T | null {
  if (!raw?.trim()) {
    return null;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export function parsePositiveId(value: number | string | null | undefined): number | null {
  if (value === undefined || value === null || value === '') {
    return null;
  }

  const numberValue = typeof value === 'number' ? value : Number.parseInt(value, 10);
  return Number.isNaN(numberValue) || numberValue <= 0 ? null : numberValue;
}
