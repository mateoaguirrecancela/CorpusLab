import { type ReactNode, useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

type AuthFormFieldProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  icon?: ReactNode
  type?: 'text' | 'email' | 'password' | 'date'
  minLength?: number
}

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
  const isPasswordField = type === 'password'
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const inputType = isPasswordField && isPasswordVisible ? 'text' : type

  return (
    <label className="stagger block">
      <span className="mb-1.5 inline-flex items-center gap-1.5 text-[0.64rem] font-bold tracking-[0.12em] text-[color:var(--cl-secondary)] uppercase">
        {icon}
        {label}
      </span>
      <div className="relative">
        <input
          autoComplete="off"
          className="h-11 w-full rounded-md border border-transparent bg-[color:var(--cl-primary-soft)] px-3 text-sm text-[color:var(--cl-neutral)] transition focus:border-[color:var(--cl-tertiary)] focus:bg-white focus:outline-none"
          id={id}
          minLength={minLength}
          name={id}
          onChange={(event) => onChange(event.currentTarget.value)}
          placeholder={placeholder}
          type={inputType}
          value={value}
        />

        {isPasswordField && (
          <button
            aria-label={isPasswordVisible ? 'Hide password' : 'Show password'}
            className="absolute top-1/2 right-3 -translate-y-1/2 text-[color:var(--cl-tertiary)] transition hover:text-[color:var(--cl-primary)]"
            onClick={() => setIsPasswordVisible((current) => !current)}
            type="button"
          >
            {isPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        )}
      </div>
    </label>
  )
}
