import { type ReactNode, useMemo, useState } from 'react';
import { ChevronsUpDown } from 'lucide-react';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { type CountryOption } from '@/modules/auth/types/signup';

type AuthComboboxProps = {
  id: string;
  label: string;
  value: string;
  options: CountryOption[];
  placeholder?: string;
  icon?: ReactNode;
  onChange: (value: string) => void;
};

export function AuthCombobox({
  id,
  label,
  value,
  options,
  placeholder = 'Select an option',
  icon,
  onChange,
}: AuthComboboxProps) {
  const [isOpen, setIsOpen] = useState(false);

  const selectedOption = useMemo(
    () => options.find((option) => option.value === value),
    [options, value],
  );

  const selectOption = (option: CountryOption) => {
    onChange(option.value);
    setIsOpen(false);
  };

  return (
    <label className="stagger relative block">
      <span className="mb-1.5 inline-flex items-center gap-1.5 text-[0.64rem] font-bold tracking-[0.12em] text-[color:var(--cl-secondary)] uppercase">
        {icon}
        {label}
      </span>

      <Popover open={isOpen} onOpenChange={setIsOpen}>
        <PopoverTrigger
          render={
            <button
              className="flex h-11 w-full items-center justify-between rounded-md border border-transparent bg-[color:var(--cl-primary-soft)] px-3 text-sm font-normal text-[color:var(--cl-neutral)] transition hover:bg-[color:var(--cl-primary-soft)] focus-visible:border-[color:var(--cl-tertiary)] focus-visible:bg-white focus-visible:outline-none"
              type="button"
            />
          }
        >
          <span
            aria-controls={`${id}-options`}
            aria-expanded={isOpen}
            className="flex w-full items-center justify-between"
            id={id}
            role="combobox"
          >
            <span className="truncate text-left">{selectedOption?.label ?? placeholder}</span>
            <ChevronsUpDown className="ml-2 size-4 shrink-0 opacity-60" />
          </span>
        </PopoverTrigger>

        <PopoverContent align="start" className="w-[var(--anchor-width)] p-0" sideOffset={6}>
          <Command>
            <CommandInput placeholder={placeholder} />
            <CommandList id={`${id}-options`}>
              <CommandEmpty>No countries found</CommandEmpty>
              <CommandGroup>
                {options.map((option) => (
                  <CommandItem
                    data-checked={option.value === value}
                    key={option.value}
                    onSelect={() => selectOption(option)}
                    value={`${option.label} ${option.value}`}
                  >
                    {option.label}
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      <input name={id} type="hidden" value={value} />
    </label>
  );
}
