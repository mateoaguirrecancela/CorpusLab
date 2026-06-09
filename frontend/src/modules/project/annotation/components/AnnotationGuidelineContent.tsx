import { useTranslation } from 'react-i18next';

type AnnotationGuidelineContentProps = Readonly<{
  guidelinePdfAvailable: boolean | undefined;
  guidelinePdfError: string;
  guidelinePdfUrl: string | null;
  guidelineText: string | null | undefined;
}>;

export function AnnotationGuidelineContent({
  guidelinePdfAvailable,
  guidelinePdfError,
  guidelinePdfUrl,
  guidelineText,
}: AnnotationGuidelineContentProps) {
  const { t } = useTranslation();

  if (guidelineText) {
    return (
      <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
        {guidelineText}
      </p>
    );
  }

  if (guidelinePdfUrl != null) {
    return (
      <iframe
        className="h-120 w-full rounded-md border border-border bg-background"
        src={guidelinePdfUrl}
        title={t('project.annotationPage.guidelineTitle')}
      />
    );
  }

  if (guidelinePdfError.length > 0) {
    return <p className="text-sm text-destructive">{guidelinePdfError}</p>;
  }

  if (guidelinePdfAvailable) {
    return (
      <p className="text-sm text-muted-foreground">
        {t('project.annotationPage.guidelinePdfLoading')}
      </p>
    );
  }

  return <p className="text-sm text-muted-foreground">{t('project.detail.noGuideline')}</p>;
}
