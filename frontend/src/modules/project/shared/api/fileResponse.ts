import { type AxiosResponse } from 'axios';
import type { ProjectDatasetItemContent } from '@/modules/project/shared/types/project';

function parseFileNameFromContentDisposition(
  contentDisposition: string | undefined,
): string | null {
  if (!contentDisposition) {
    return null;
  }

  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  const utf8FileName = utf8Match?.[1];
  if (utf8FileName) {
    try {
      return decodeURIComponent(utf8FileName);
    } catch {
      return utf8FileName;
    }
  }

  const quotedMatch = /filename="([^"]+)"/i.exec(contentDisposition);
  const quotedFileName = quotedMatch?.[1];
  if (quotedFileName) {
    return quotedFileName;
  }

  const plainMatch = /filename=([^;]+)/i.exec(contentDisposition);
  const plainFileName = plainMatch?.[1];
  if (plainFileName) {
    return plainFileName.trim();
  }

  return null;
}

function getResponseHeader(
  response: AxiosResponse<ArrayBuffer>,
  headerName: string,
): string | undefined {
  const headerValue = response.headers[headerName];
  return typeof headerValue === 'string' && headerValue.length > 0 ? headerValue : undefined;
}

export function toDatasetItemContent(
  response: AxiosResponse<ArrayBuffer>,
  fallbackMimeType: string,
): ProjectDatasetItemContent {
  const mimeType = getResponseHeader(response, 'content-type') ?? fallbackMimeType;

  return {
    blob: new Blob([response.data], { type: mimeType }),
    mimeType,
    fileName: parseFileNameFromContentDisposition(
      getResponseHeader(response, 'content-disposition'),
    ),
  };
}
