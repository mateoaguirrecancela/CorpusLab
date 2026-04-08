import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { LABEL_COLOR_PALETTE } from '@/modules/project/constants/labelColorPalette';

type LabelEditorDialogProps = Readonly<{
  colorLabel: string;
  currentColor: string;
  currentName: string;
  inputLabel: string;
  isNerProjectType: boolean;
  isOpen: boolean;
  mode: 'create' | 'edit';
  onColorChange: (nextColor: string) => void;
  onNameChange: (nextName: string) => void;
  onOpenChange: (open: boolean) => void;
  onSave: () => void;
  placeholder: string;
  saveCreateText: string;
  saveEditText: string;
  titleCreate: string;
  titleEdit: string;
}>;

export function LabelEditorDialog({
  colorLabel,
  currentColor,
  currentName,
  inputLabel,
  isNerProjectType,
  isOpen,
  mode,
  onColorChange,
  onNameChange,
  onOpenChange,
  onSave,
  placeholder,
  saveCreateText,
  saveEditText,
  titleCreate,
  titleEdit,
}: LabelEditorDialogProps) {
  return (
    <Dialog onOpenChange={onOpenChange} open={isOpen}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{mode === 'create' ? titleCreate : titleEdit}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <FormFieldControl
            id="modal-label-name"
            inputProps={{
              autoFocus: true,
              maxLength: 128,
              placeholder,
              required: true,
            }}
            label={inputLabel}
            onValueChange={onNameChange}
            required
            value={currentName}
          />

          {isNerProjectType && (
            <div className="space-y-3">
              <p className="text-xs font-bold tracking-widest uppercase text-muted-foreground">
                {colorLabel} *
              </p>

              {/* Paleta curada SaaS */}
              <div className="flex flex-wrap gap-2.5">
                {LABEL_COLOR_PALETTE.map((color) => {
                  const isSelected = currentColor.toLowerCase() === color.toLowerCase();
                  return (
                    <button
                      key={color}
                      type="button"
                      aria-label={`Select color ${color}`}
                      onClick={() => onColorChange(color)}
                      className={`size-8 rounded-full transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 ${
                        isSelected
                          ? 'scale-110 ring-2 ring-primary ring-offset-2 ring-offset-background'
                          : 'hover:scale-110 opacity-90 hover:opacity-100 border border-border/50'
                      }`}
                      style={{ backgroundColor: color }}
                    />
                  );
                })}
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button
            className="h-10 min-w-28 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            onClick={onSave}
            type="button"
          >
            {mode === 'create' ? saveCreateText : saveEditText}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
