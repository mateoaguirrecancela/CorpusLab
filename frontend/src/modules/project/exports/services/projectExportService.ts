import { api } from '@/app/config/axiosInstance';
import { toDatasetItemContent } from '@/modules/project/shared/api/fileResponse';
import type { ProjectDatasetItemContent } from '@/modules/project/shared/types/project';

export async function exportProjectAnnotationResultsCsv(
  projectId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(`/projects/${projectId}/annotations/export`, {
    responseType: 'arraybuffer',
  });

  return toDatasetItemContent(response, 'text/csv;charset=UTF-8');
}
