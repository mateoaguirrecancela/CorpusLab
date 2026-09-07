import { beforeEach, describe, expect, it, vi } from 'vitest';
import { type TFunction } from 'i18next';
import { toast } from 'sonner';
import { validateProjectFiles } from '@/modules/project/setup/utils/projectCreateValidation';

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

const t = ((key: string, opts?: Record<string, unknown>) =>
  opts ? `${key}:${JSON.stringify(opts)}` : key) as TFunction;

function file(name: string, sizeBytes: number, type = 'text/plain'): File {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
});

describe('validateProjectFiles', () => {
  it('accepts valid new files and merges them with the current ones', () => {
    const result = validateProjectFiles({
      currentFiles: [file('a.txt', 10)],
      incomingFiles: [file('b.txt', 10)],
      t,
    });

    expect(result).toHaveLength(2);
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('rejects a file exceeding the per-file size limit', () => {
    const result = validateProjectFiles({
      currentFiles: [],
      incomingFiles: [file('big.txt', 11 * 1024 * 1024)],
      t,
    });

    expect(result).toBeNull();
    expect(toast.error).toHaveBeenCalled();
  });

  it('rejects a banned extension', () => {
    const result = validateProjectFiles({
      currentFiles: [],
      incomingFiles: [file('script.exe', 10)],
      t,
    });

    expect(result).toBeNull();
  });

  it('rejects a duplicate of an existing file (same name and size)', () => {
    const result = validateProjectFiles({
      currentFiles: [file('a.txt', 10)],
      incomingFiles: [file('a.txt', 10)],
      t,
    });

    expect(result).toBeNull();
  });

  it('rejects duplicates within the incoming batch itself', () => {
    const result = validateProjectFiles({
      currentFiles: [],
      incomingFiles: [file('a.txt', 10), file('a.txt', 10)],
      t,
    });

    expect(result).toBeNull();
  });

  it('rejects when the combined total size exceeds the limit', () => {
    const result = validateProjectFiles({
      currentFiles: [file('a.txt', 40 * 1024 * 1024)],
      incomingFiles: [file('b.txt', 20 * 1024 * 1024)],
      t,
    });

    expect(result).toBeNull();
  });

  it('rejects mixing a csv file with any other file', () => {
    const result = validateProjectFiles({
      currentFiles: [file('a.csv', 10, 'text/csv')],
      incomingFiles: [file('b.txt', 10)],
      t,
    });

    expect(result).toBeNull();
  });

  it('accepts a single csv file on its own', () => {
    const result = validateProjectFiles({
      currentFiles: [],
      incomingFiles: [file('a.csv', 10, 'text/csv')],
      t,
    });

    expect(result).toHaveLength(1);
  });
});
