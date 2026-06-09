import { useTranslation } from 'react-i18next';
import { LabelEditorDialog } from '@/modules/project/setup/components/LabelEditorDialog';
import { ProjectTypeSelector } from '@/modules/project/setup/components/ProjectTypeSelector';
import { ProjectSetupAnnotationTargetField } from '@/modules/project/setup/components/sections/ProjectSetupAnnotationTargetField';
import { ProjectSetupFooterActions } from '@/modules/project/setup/components/sections/ProjectSetupFooterActions';
import { ProjectSetupGuidelineSection } from '@/modules/project/setup/components/sections/ProjectSetupGuidelineSection';
import { ProjectSetupLabelsSection } from '@/modules/project/setup/components/sections/ProjectSetupLabelsSection';
import { useProjectSetupStep } from '@/modules/project/setup/hooks/useProjectSetupStep';
import { type ConfigureProjectSetupPayload } from '@/modules/project/shared/types/project';

type ProjectSetupStepProps = Readonly<{
  datasetFiles: File[];
  onBack: () => void;
  onCompleted: (payload: ConfigureProjectSetupPayload) => void | Promise<void>;
}>;

export function ProjectSetupStep({ datasetFiles, onBack, onCompleted }: ProjectSetupStepProps) {
  const { t } = useTranslation();
  const {
    annotationTargetColumn,
    canSaveSetup,
    canUseCsvColumnsAsLabels,
    csvHeaderOptions,
    guidelineMode,
    guidelinePdfFile,
    guidelineText,
    handleAnnotationTargetColumnChange,
    handleGuidelineModeChange,
    handleGuidelinePdfRejected,
    handleGuidelinePdfRemove,
    handleGuidelinePdfSelected,
    handleGuidelineTextChange,
    handleProjectTypeChange,
    handleSaveProjectSetup,
    handleUseCsvColumnsAsLabelsChange,
    isCsvLabelModeEnabled,
    isLoadingCsvHeaders,
    isNerDatasetCompatible,
    isNerProjectType,
    isPreparingSetup,
    labelEditor,
    labels,
    projectType,
    requiresAnnotationTargetColumn,
    requiresLabels,
    setupErrors,
    shouldShowClassificationLabelMinError,
  } = useProjectSetupStep({ datasetFiles, onCompleted });

  return (
    <div className="mt-8 space-y-6">
      <section className="space-y-3">
        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.create.projectTypeLabel')} *
        </p>

        <ProjectTypeSelector
          disabledTypes={isNerDatasetCompatible === false ? ['NER'] : []}
          onChange={handleProjectTypeChange}
          value={projectType}
        />
      </section>

      <ProjectSetupAnnotationTargetField
        annotationTargetColumn={annotationTargetColumn}
        csvHeaderOptions={csvHeaderOptions}
        errorMessage={setupErrors.annotationTargetColumn?.message}
        isLoadingCsvHeaders={isLoadingCsvHeaders}
        onChange={handleAnnotationTargetColumnChange}
        requiresAnnotationTargetColumn={requiresAnnotationTargetColumn}
      />

      {requiresLabels && (
        <ProjectSetupLabelsSection
          canUseCsvColumnsAsLabels={canUseCsvColumnsAsLabels}
          isNerProjectType={isNerProjectType}
          labelErrorMessage={setupErrors.labels?.message}
          labels={labels}
          onAddLabel={labelEditor.openCreateLabelDialog}
          onEditLabel={labelEditor.openEditLabelDialog}
          onRemoveLabel={labelEditor.removeLabelAt}
          onUseCsvColumnsAsLabelsChange={handleUseCsvColumnsAsLabelsChange}
          shouldShowClassificationLabelMinError={shouldShowClassificationLabelMinError}
          useCsvColumnsAsLabels={isCsvLabelModeEnabled}
        />
      )}

      <ProjectSetupGuidelineSection
        guidelineMode={guidelineMode}
        guidelinePdfErrorMessage={setupErrors.guidelinePdfFile?.message}
        guidelinePdfFile={guidelinePdfFile}
        guidelineText={guidelineText}
        guidelineTextErrorMessage={setupErrors.guidelineText?.message}
        onGuidelineModeChange={handleGuidelineModeChange}
        onGuidelinePdfRejected={handleGuidelinePdfRejected}
        onGuidelinePdfRemove={handleGuidelinePdfRemove}
        onGuidelinePdfSelected={handleGuidelinePdfSelected}
        onGuidelineTextChange={handleGuidelineTextChange}
      />

      <ProjectSetupFooterActions
        canSaveSetup={canSaveSetup}
        isPreparingSetup={isPreparingSetup}
        onBack={onBack}
        onSave={() => void handleSaveProjectSetup()}
      />

      <LabelEditorDialog
        colorLabel={t('project.create.labelColor')}
        currentColor={labelEditor.draftLabelColor}
        currentName={labelEditor.draftLabelName}
        inputLabel={t('project.create.labelName')}
        isNerProjectType={isNerProjectType}
        isOpen={labelEditor.isLabelEditorOpen}
        mode={labelEditor.labelDialogMode}
        onColorChange={labelEditor.setDraftLabelColor}
        onNameChange={labelEditor.setDraftLabelName}
        onOpenChange={labelEditor.setIsLabelEditorOpen}
        onSave={labelEditor.saveLabelFromDialog}
        isSaveDisabled={labelEditor.isLabelDialogSaveDisabled}
        placeholder={
          isCsvLabelModeEnabled
            ? t('project.create.csvLabelNamePlaceholder')
            : t('project.create.labelNamePlaceholder')
        }
        labelNameOptions={labelEditor.csvLabelNameOptions}
        saveCreateText={t('project.create.addLabel')}
        saveEditText={t('project.create.saveLabel')}
        titleCreate={t('project.create.createLabelTitle')}
        titleEdit={t('project.create.editLabelTitle')}
      />
    </div>
  );
}
