import { type FormEvent, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  type DefaultValues,
  type FieldValues,
  type Path,
  type PathValue,
  type Resolver,
  useForm,
  useWatch,
} from 'react-hook-form';
import { type z } from 'zod';
import { type FieldKey, mergeFormValues, pickFieldErrors } from '@/lib/formUtils';

type UseAuthFormControllerOptions<TValues extends FieldValues> = {
  defaultValues: TValues;
  fieldNames: readonly FieldKey<TValues>[];
  schema: z.ZodType<TValues, TValues>;
  initialErrorMessage?: string;
};

type SubmitFormOptions<TValues extends FieldValues> = {
  event: FormEvent<HTMLFormElement>;
  invalidMessage: string;
  onError: (error: unknown) => void;
  onValid: (values: TValues) => Promise<void>;
};

export function useAuthFormController<TValues extends FieldValues>({
  defaultValues,
  fieldNames,
  schema,
  initialErrorMessage = '',
}: UseAuthFormControllerOptions<TValues>) {
  const methods = useForm<TValues>({
    defaultValues: defaultValues as DefaultValues<TValues>,
    mode: 'onChange',
    resolver: zodResolver(schema) as Resolver<TValues>,
  });
  const {
    control,
    formState: { errors, isValid },
    getValues,
    setValue,
    trigger,
  } = methods;
  const watchedValues = useWatch({ control });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(initialErrorMessage);
  const [successMessage, setSuccessMessage] = useState('');

  const form = mergeFormValues(defaultValues, watchedValues as Partial<TValues>);
  const fieldErrors = pickFieldErrors(errors, fieldNames);
  const canSubmit = isValid && !isSubmitting;

  const updateField = <TField extends FieldKey<TValues>>(field: TField, value: TValues[TField]) => {
    const fieldPath = field as unknown as Path<TValues>;
    setValue(fieldPath, value as PathValue<TValues, typeof fieldPath>, {
      shouldDirty: true,
      shouldValidate: true,
    });
  };

  const submitForm = async ({
    event,
    invalidMessage,
    onError,
    onValid,
  }: SubmitFormOptions<TValues>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const isFormValid = await trigger();
    if (!isFormValid) {
      setErrorMessage(invalidMessage);
      setSuccessMessage('');
      return;
    }

    const values = schema.parse(getValues());
    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await onValid(values);
    } catch (error) {
      onError(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    methods,
    form,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    setErrorMessage,
    setSuccessMessage,
    submitForm,
    updateField,
  };
}
