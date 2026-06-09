import { useEffect, useState } from 'react';
import {
  getProjectGuidelinePdf,
  getProjectGuidelinePdfErrorMessage,
} from '@/modules/project/shared/services/projectService';
import type { ProjectDetail } from '@/modules/project/shared/types/project';

type GuidelinePdfState = Readonly<{
  guidelinePdfError: string;
  guidelinePdfUrl: string | null;
}>;

type GuidelinePdfLoadState = Readonly<{
  error: string;
  projectId: number | null;
  url: string | null;
}>;

export function useGuidelinePdf(project: ProjectDetail | undefined): GuidelinePdfState {
  const [loadState, setLoadState] = useState<GuidelinePdfLoadState>({
    error: '',
    projectId: null,
    url: null,
  });

  useEffect(() => {
    let createdGuidelineUrl: string | null = null;
    let cancelled = false;

    if (!project?.guidelinePdfAvailable) {
      return;
    }

    void getProjectGuidelinePdf(project.id)
      .then((guidelinePdf) => {
        if (cancelled) {
          return;
        }

        createdGuidelineUrl = URL.createObjectURL(guidelinePdf.blob);
        setLoadState({
          error: '',
          projectId: project.id,
          url: createdGuidelineUrl,
        });
      })
      .catch((loadError) => {
        if (!cancelled) {
          setLoadState({
            error: getProjectGuidelinePdfErrorMessage(loadError),
            projectId: project.id,
            url: null,
          });
        }
      });

    return () => {
      cancelled = true;
      if (createdGuidelineUrl != null) {
        URL.revokeObjectURL(createdGuidelineUrl);
      }
    };
  }, [project?.guidelinePdfAvailable, project?.id]);

  const isCurrentProject = project?.guidelinePdfAvailable && loadState.projectId === project.id;

  return {
    guidelinePdfError: isCurrentProject ? loadState.error : '',
    guidelinePdfUrl: isCurrentProject ? loadState.url : null,
  };
}
