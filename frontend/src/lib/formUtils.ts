import { type FieldErrors, type FieldValues } from 'react-hook-form';

export type FieldKey<TValues extends FieldValues> = Extract<keyof TValues, string>;

export function mergeFormValues<TValues extends FieldValues>(
  defaultValues: TValues,
  watchedValues: Partial<TValues>,
): TValues {
  return {
    ...defaultValues,
    ...watchedValues,
  };
}

export function pickFieldErrors<TValues extends FieldValues>(
  errors: FieldErrors<TValues>,
  fieldNames: readonly FieldKey<TValues>[],
): Partial<Record<FieldKey<TValues>, string>> {
  const fieldErrors: Partial<Record<FieldKey<TValues>, string>> = {};

  for (const fieldName of fieldNames) {
    const message = errors[fieldName]?.message;
    if (typeof message === 'string') {
      fieldErrors[fieldName] = message;
    }
  }

  return fieldErrors;
}
