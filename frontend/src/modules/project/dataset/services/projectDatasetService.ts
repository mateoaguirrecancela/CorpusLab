import { api } from '@/app/config/axiosInstance';
import i18n from '@/app/config/i18n';
import { getSessionToken } from '@/modules/auth/services/sessionService';
import { toDatasetItemContent } from '@/modules/project/shared/api/fileResponse';
import type {
  ProjectDatasetItemContent,
  UploadDatasetEvent,
  UploadDatasetJobResponse,
  UploadDatasetResponse,
} from '@/modules/project/shared/types/project';

const DATASET_UPLOAD_TIMEOUT_MS = 15 * 60 * 1000;

export async function uploadProjectDataset(
  groupId: number,
  projectId: number,
  files: File[],
): Promise<UploadDatasetResponse> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<UploadDatasetJobResponse>(
    `/research-groups/${groupId}/projects/${projectId}/dataset`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return waitForDatasetUploadCompletion(projectId, response.data.jobId);
}

async function waitForDatasetUploadCompletion(
  projectId: number,
  jobId: string,
): Promise<UploadDatasetResponse> {
  const token = getSessionToken();
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';
  const controller = new AbortController();
  let timedOut = false;
  const timeoutId = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, DATASET_UPLOAD_TIMEOUT_MS);

  try {
    const response = await fetch(`/api/projects/${projectId}/dataset/upload-events/${jobId}`, {
      headers: {
        Accept: 'text/event-stream',
        'Accept-Language': language,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal: controller.signal,
    });

    if (!response.ok || !response.body) {
      throw new Error(i18n.t('project.errors.datasetUploadFailed'));
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split('\n\n');
      buffer = events.pop() ?? '';

      for (const rawEvent of events) {
        const uploadEvent = parseDatasetUploadEvent(rawEvent);
        if (!uploadEvent) {
          continue;
        }

        if (uploadEvent.status === 'COMPLETED' && uploadEvent.result) {
          return uploadEvent.result;
        }

        if (uploadEvent.status === 'FAILED') {
          throw new Error(uploadEvent.message ?? i18n.t('project.errors.datasetUploadFailed'));
        }
      }
    }

    throw new Error(i18n.t('project.errors.datasetUploadFailed'));
  } catch (error) {
    if (timedOut || (error instanceof DOMException && error.name === 'AbortError')) {
      throw new Error(i18n.t('project.errors.datasetUploadFailed'));
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

function parseDatasetUploadEvent(rawEvent: string): UploadDatasetEvent | null {
  const data = rawEvent
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trimStart())
    .join('\n');

  if (!data) {
    return null;
  }

  return JSON.parse(data) as UploadDatasetEvent;
}

export async function getProjectDatasetItemContent(
  projectId: number,
  datasetItemId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(
    `/projects/${projectId}/dataset-items/${datasetItemId}/content`,
    {
      responseType: 'arraybuffer',
    },
  );

  return toDatasetItemContent(response, 'application/octet-stream');
}
