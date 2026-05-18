import { useMemo, useState } from 'react';
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

type CountryComboboxProps = {
  id: string;
  value: string;
  options: CountryOption[];
  placeholder?: string;
  onChange: (value: string) => void;
};

export function CountryCombobox({
  id,
  value,
  options,
  placeholder = 'Select an option',
  onChange,
}: CountryComboboxProps) {
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
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger
        render={
          <button
            className="flex h-10 w-full items-center justify-between rounded-md border border-input bg-surface-base px-3 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50"
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
  );
}
