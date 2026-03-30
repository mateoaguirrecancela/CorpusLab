import { type ReactNode } from 'react';
import { ChevronDown } from 'lucide-react';
import { type CountryOption } from '@/modules/auth/types/signup';

type AuthSelectFieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: CountryOption[];
  icon?: ReactNode;
};

export function AuthSelectField({
  id,
  label,
  value,
  onChange,
  options,
  icon,
}: AuthSelectFieldProps) {
  return (
    <label className="stagger block">
      <span className="mb-1.5 inline-flex items-center gap-1.5 text-[0.64rem] font-bold tracking-[0.12em] text-[color:var(--cl-secondary)] uppercase">
        {icon}
        {label}
      </span>
      <div className="relative">
        <select
          className="h-11 w-full appearance-none rounded-md border border-transparent bg-[color:var(--cl-primary-soft)] px-3 pr-10 text-sm text-[color:var(--cl-neutral)] transition focus:border-[color:var(--cl-tertiary)] focus:bg-white focus:outline-none"
          id={id}
          name={id}
          onChange={(event) => onChange(event.currentTarget.value)}
          value={value}
        >
          {options.map((option) => (
            <option key={option.label} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-[color:var(--cl-tertiary)]" />
      </div>
    </label>
  );
}
