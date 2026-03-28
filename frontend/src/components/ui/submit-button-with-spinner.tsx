import { type ComponentProps } from 'react'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'

type SubmitButtonWithSpinnerProps = Omit<ComponentProps<typeof Button>, 'children' | 'type'> & {
  isSubmitting: boolean
  idleLabel: string
  submittingLabel: string
  spinnerClassName?: string
}

export function SubmitButtonWithSpinner({
  isSubmitting,
  idleLabel,
  submittingLabel,
  spinnerClassName,
  disabled,
  ...props
}: SubmitButtonWithSpinnerProps) {
  return (
    <Button type="submit" disabled={disabled || isSubmitting} {...props}>
      {isSubmitting ? (
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className={spinnerClassName ?? 'size-4'} />
          {submittingLabel}
        </span>
      ) : (
        idleLabel
      )}
    </Button>
  )
}
