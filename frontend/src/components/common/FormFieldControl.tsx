import { type ReactNode, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

import { Field, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/shared/utils/cn';

type SelectOption = {
  label: string;
  value: string;
  disabled?: boolean;
};

type BaseProps = {
  id: string;
  label: string;
  icon?: ReactNode;
  required?: boolean;
  message?: string;
  className?: string;
  labelClassName?: string;
  controlClassName?: string;
  messageClassName?: string;
};

type InputControlProps = {
  controlType?: 'input';
  inputType?: React.ComponentProps<'input'>['type'];
  value: string;
  onValueChange: (value: string) => void;
  inputProps?: Omit<
    React.ComponentProps<'input'>,
    'id' | 'value' | 'onChange' | 'type' | 'className'
  >;
  showPasswordLabel?: string;
  hidePasswordLabel?: string;
};

type TextareaControlProps = {
  controlType: 'textarea';
  value: string;
  onValueChange: (value: string) => void;
  textareaProps?: Omit<React.ComponentProps<'textarea'>, 'id' | 'value' | 'onChange' | 'className'>;
};

type SelectControlProps = {
  controlType: 'select';
  value: string;
  onValueChange: (value: string) => void;
  options?: SelectOption[];
  children?: ReactNode;
  selectProps?: Omit<React.ComponentProps<'select'>, 'id' | 'value' | 'onChange' | 'className'>;
};

type CustomControlProps = {
  controlType: 'custom';
  renderControl: (props: { id: string; className?: string }) => ReactNode;
};

type FormFieldControlProps =
  | (BaseProps & InputControlProps)
  | (BaseProps & TextareaControlProps)
  | (BaseProps & SelectControlProps)
  | (BaseProps & CustomControlProps);

export function FormFieldControl(props: FormFieldControlProps) {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const {
    className,
    controlClassName,
    icon,
    id,
    label,
    labelClassName,
    message,
    messageClassName,
    required,
  } = props;

  function renderControl() {
    if (props.controlType === 'textarea') {
      return (
        <Textarea
          className={controlClassName}
          id={id}
          onChange={(event) => props.onValueChange(event.currentTarget.value)}
          value={props.value}
          {...props.textareaProps}
        />
      );
    }

    if (props.controlType === 'select') {
      return (
        <Select
          className={controlClassName}
          id={id}
          onChange={(event) => props.onValueChange(event.currentTarget.value)}
          value={props.value}
          {...props.selectProps}
        >
          {props.options?.map((option) => (
            <option
              disabled={option.disabled}
              key={`${option.value}-${option.label}`}
              value={option.value}
            >
              {option.label}
            </option>
          ))}
          {props.children}
        </Select>
      );
    }

    if (props.controlType === 'custom') {
      return props.renderControl({ className: controlClassName, id });
    }

    const inputType = props.inputType ?? 'text';

    if (inputType === 'password') {
      const toggleAriaLabel = isPasswordVisible
        ? (props.hidePasswordLabel ?? 'Hide password')
        : (props.showPasswordLabel ?? 'Show password');

      return (
        <div className="relative">
          <Input
            className={controlClassName}
            id={id}
            onChange={(event) => props.onValueChange(event.currentTarget.value)}
            type={isPasswordVisible ? 'text' : 'password'}
            value={props.value}
            {...props.inputProps}
          />
          <button
            aria-label={toggleAriaLabel}
            className="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground transition hover:text-primary"
            onClick={() => setIsPasswordVisible((current) => !current)}
            type="button"
          >
            {isPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      );
    }

    return (
      <Input
        className={controlClassName}
        id={id}
        onChange={(event) => props.onValueChange(event.currentTarget.value)}
        type={inputType}
        value={props.value}
        {...props.inputProps}
      />
    );
  }

  return (
    <div className={className}>
      <Field>
        <FieldLabel className={labelClassName} htmlFor={id}>
          {icon}
          {required ? `${label} *` : label}
        </FieldLabel>
        {renderControl()}
      </Field>

      {message ? (
        <p className={cn('mt-2 text-xs text-destructive', messageClassName)}>{message}</p>
      ) : null}
    </div>
  );
}
