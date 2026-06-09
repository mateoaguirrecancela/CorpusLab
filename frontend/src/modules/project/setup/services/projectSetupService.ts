import { api } from '@/app/config/axiosInstance';
import { toDatasetItemContent } from '@/modules/project/shared/api/fileResponse';
import type {
  ConfigureProjectSetupPayload,
  ProjectDatasetItemContent,
  ProjectSetupResponse,
} from '@/modules/project/shared/types/project';
import { optionalTrimmedText } from '@/modules/project/shared/utils/projectFormUtils';

export async function configureProjectSetup(
  groupId: number,
  projectId: number,
  payload: ConfigureProjectSetupPayload,
): Promise<ProjectSetupResponse> {
  const request = {
    projectType: payload.projectType,
    labels: payload.labels,
    guidelineText: optionalTrimmedText(payload.guidelineText),
    annotationTargetColumn: optionalTrimmedText(payload.annotationTargetColumn),
  };
  const formData = new FormData();
  formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));

  if (payload.guidelinePdfFile) {
    formData.append('guidelinePdfFile', payload.guidelinePdfFile);
  }

  const response = await api.put<ProjectSetupResponse>(
    `/research-groups/${groupId}/projects/${projectId}/setup`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return response.data;
}

export async function getProjectGuidelinePdf(
  projectId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(`/projects/${projectId}/guideline-pdf`, {
    responseType: 'arraybuffer',
  });

  return toDatasetItemContent(response, 'application/pdf');
}
