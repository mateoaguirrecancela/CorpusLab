import { type LucideIcon, CheckCircle2, ListChecks, Highlighter, TextCursorInput } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { type ProjectType } from '@/modules/project/types/project';

interface ProjectTypeOption {
  type: ProjectType;
  icon: LucideIcon;
  titleKey: string;
  descriptionKey: string;
}

const PROJECT_TYPES: ProjectTypeOption[] = [
  {
    type: 'TEXT_CLASSIFICATION_SIMPLE',
    icon: CheckCircle2,
    titleKey: 'project.create.projectTypes.textClassificationSimple',
    descriptionKey: 'project.create.projectTypesDescriptions.textClassificationSimple',
  },
  {
    type: 'TEXT_CLASSIFICATION_MULTILABEL',
    icon: ListChecks,
    titleKey: 'project.create.projectTypes.textClassificationMultiLabel',
    descriptionKey: 'project.create.projectTypesDescriptions.textClassificationMultiLabel',
  },
  {
    type: 'NER',
    icon: Highlighter,
    titleKey: 'project.create.projectTypes.ner',
    descriptionKey: 'project.create.projectTypesDescriptions.ner',
  },
  {
    type: 'SEQ2SEQ',
    icon: TextCursorInput,
    titleKey: 'project.create.projectTypes.seq2seq',
    descriptionKey: 'project.create.projectTypesDescriptions.seq2seq',
  },
];

interface ProjectTypeSelectorProps {
  value: ProjectType;
  onChange: (value: ProjectType) => void;
  disabledTypes?: ProjectType[];
}

export function ProjectTypeSelector({ value, onChange, disabledTypes = [] }: ProjectTypeSelectorProps) {
  const { t } = useTranslation();

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      {PROJECT_TYPES.map((option) => {
        const isSelected = value === option.type;
        const isDisabled = disabledTypes.includes(option.type);
        const Icon = option.icon;

        return (
          <button
            key={option.type}
            className={`
              relative flex items-center gap-4 rounded-xl border-2 p-5 text-left transition-all duration-200 group
              ${
                isSelected
                  ? 'border-primary bg-primary/5 shadow-[0_0_0_1px_var(--color-primary)]'
                  : 'border-border bg-surface-base hover:border-primary/40 hover:bg-primary/[0.02]'
              }
              ${isDisabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}
            `}
            disabled={isDisabled}
            onClick={() => !isDisabled && onChange(option.type)}
            type="button"
          >
            <div
              className={`
              flex size-11 shrink-0 items-center justify-center rounded-xl border-2 transition-all duration-200
              ${
                isSelected
                  ? 'bg-primary text-primary-foreground border-primary shadow-md'
                  : 'bg-muted/50 text-muted-foreground border-border group-hover:border-primary/30 group-hover:bg-primary/5'
              }
            `}
            >
              <Icon className={`size-5 transition-transform duration-200 ${isSelected ? 'scale-110' : 'group-hover:scale-110'}`} />
            </div>

            <div className="flex flex-col gap-0.5">
              <span
                className={`text-[15px] font-bold tracking-tight transition-colors ${
                  isSelected ? 'text-primary' : 'text-foreground'
                }`}
              >
                {t(option.titleKey)}
              </span>
              <span className="text-xs leading-relaxed text-muted-foreground/90 line-clamp-2">
                {t(option.descriptionKey)}
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
}
