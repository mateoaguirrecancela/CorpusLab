export type Base64FilePayload = Readonly<{
  mimeType: string;
  base64Payload: string;
}>;

export function parseBase64FilePayload(
  rawValue: string,
  fallbackMimeType: string,
): Base64FilePayload {
  const trimmedValue = rawValue.trim();
  const match = /^data:([^;]+);base64,(.+)$/i.exec(trimmedValue);
  if (!match) {
    return {
      mimeType: fallbackMimeType,
      base64Payload: trimmedValue,
    };
  }

  return {
    mimeType: match[1],
    base64Payload: match[2],
  };
}

export function calculateBase64SizeBytes(base64Payload: string): number {
  try {
    return globalThis.atob(base64Payload).length;
  } catch {
    return 0;
  }
}

export function decodeBase64ToBuffer(base64Payload: string): ArrayBuffer {
  const binary = globalThis.atob(base64Payload);
  const buffer = new ArrayBuffer(binary.length);
  const bytes = new Uint8Array(buffer);

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.codePointAt(index) ?? 0;
  }

  return buffer;
}

export function triggerBlobDownload(blob: Blob, fileName: string): void {
  const blobUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = blobUrl;
  anchor.download = fileName;
  anchor.style.display = 'none';

  document.body.append(anchor);
  anchor.click();
  anchor.remove();

  setTimeout(() => {
    URL.revokeObjectURL(blobUrl);
  }, 60_000);
}
