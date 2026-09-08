import { Pencil, Trash2 } from 'lucide-react';
import { type ProjectSetupLabel } from '@/modules/project/shared/types/project';

type LabelRowProps = Readonly<{
  editLabelText: string;
  isNerProjectType: boolean;
  label: ProjectSetupLabel;
  onEdit: () => void;
  onRemove: () => void;
  removeLabelText: string;
}>;

export function LabelRow({
  editLabelText,
  isNerProjectType,
  label,
  onEdit,
  onRemove,
  removeLabelText,
}: LabelRowProps) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border border-border bg-surface-base/60 px-4 py-3">
      <p className="min-w-0 flex-1 truncate text-sm font-semibold text-primary">{label.name}</p>

      <div className="ml-3 inline-flex items-center gap-2">
        {isNerProjectType && label.color ? (
          <div className="inline-flex items-center gap-2 rounded-full border border-border px-2 py-1">
            <span
              className="size-3 rounded-full border border-border"
              style={{ backgroundColor: label.color }}
            />
            <span className="hidden text-xs font-medium text-muted-foreground lg:inline">
              {label.color}
            </span>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">-</span>
        )}

        <div className="inline-flex items-center gap-1">
          <button
            aria-label={editLabelText}
            className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-primary"
            onClick={onEdit}
            type="button"
          >
            <Pencil className="size-4" />
          </button>
          <button
            aria-label={removeLabelText}
            className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-destructive"
            onClick={onRemove}
            type="button"
          >
            <Trash2 className="size-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
