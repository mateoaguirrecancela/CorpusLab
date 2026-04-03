import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';

type AuthFormFieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  icon?: ReactNode;
  type?: 'text' | 'email' | 'password' | 'date';
  minLength?: number;
};

export function AuthFormField({
  id,
  label,
  value,
  onChange,
  placeholder,
  icon,
  type = 'text',
  minLength,
}: AuthFormFieldProps) {
  const { t } = useTranslation();

  return (
    <FormFieldControl
      icon={icon}
      id={id}
      inputProps={{ autoComplete: 'off', minLength, name: id, placeholder }}
      inputType={type}
      label={label}
      onValueChange={onChange}
      showPasswordLabel={t('common.aria.showPassword')}
      hidePasswordLabel={t('common.aria.hidePassword')}
      value={value}
    />
  );
}
