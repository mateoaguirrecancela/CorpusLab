import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { z } from 'zod';
import { useAuthFormController } from '@/modules/auth/hooks/useAuthFormController';

const testSchema = z
  .object({
    email: z.string().trim().min(1, 'required').email('invalid-email'),
    name: z.string().trim().min(1, 'required'),
  })
  .strict();

type TestValues = z.infer<typeof testSchema>;

const DEFAULT_VALUES: TestValues = { email: '', name: '' };
const FIELDS = ['email', 'name'] as const;

function renderController() {
  return renderHook(() =>
    useAuthFormController<TestValues>({
      defaultValues: DEFAULT_VALUES,
      fieldNames: FIELDS,
      schema: testSchema,
    }),
  );
}

function fakeEvent() {
  return { preventDefault: vi.fn() } as unknown as React.FormEvent<HTMLFormElement>;
}

describe('useAuthFormController', () => {
  it('becomes submittable once every field passes validation', async () => {
    const { result } = renderController();

    expect(result.current.canSubmit).toBe(false);

    act(() => result.current.updateField('email', 'a@b.com'));
    act(() => result.current.updateField('name', 'Jane'));

    await waitFor(() => expect(result.current.canSubmit).toBe(true));
    expect(result.current.form).toEqual({ email: 'a@b.com', name: 'Jane' });
  });

  it('surfaces field-level errors after an invalid change', async () => {
    const { result } = renderController();

    act(() => result.current.updateField('email', 'not-an-email'));

    await waitFor(() => expect(result.current.fieldErrors.email).toBe('invalid-email'));
  });

  it('rejects submission of an invalid form without calling onValid', async () => {
    const { result } = renderController();
    const onValid = vi.fn();
    const onError = vi.fn();
    const event = fakeEvent();

    await act(async () => {
      await result.current.submitForm({
        event,
        invalidMessage: 'fill everything in',
        onError,
        onValid,
      });
    });

    expect(event.preventDefault).toHaveBeenCalled();
    expect(onValid).not.toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('fill everything in');
  });

  it('parses and submits valid values, toggling isSubmitting around the call', async () => {
    const { result } = renderController();
    act(() => result.current.updateField('email', 'a@b.com'));
    act(() => result.current.updateField('name', 'Jane'));
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    let resolveOnValid: () => void = () => {};
    const onValid = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveOnValid = resolve;
        }),
    );

    let submitPromise!: Promise<void>;
    act(() => {
      submitPromise = result.current.submitForm({
        event: fakeEvent(),
        invalidMessage: 'invalid',
        onError: vi.fn(),
        onValid,
      });
    });

    await waitFor(() => expect(result.current.isSubmitting).toBe(true));
    expect(onValid).toHaveBeenCalledWith({ email: 'a@b.com', name: 'Jane' });

    await act(async () => {
      resolveOnValid();
      await submitPromise;
    });

    expect(result.current.isSubmitting).toBe(false);
  });

  it('routes a thrown error from onValid to onError and clears isSubmitting', async () => {
    const { result } = renderController();
    act(() => result.current.updateField('email', 'a@b.com'));
    act(() => result.current.updateField('name', 'Jane'));
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    const failure = new Error('server error');
    const onError = vi.fn();

    await act(async () => {
      await result.current.submitForm({
        event: fakeEvent(),
        invalidMessage: 'invalid',
        onError,
        onValid: () => Promise.reject(failure),
      });
    });

    expect(onError).toHaveBeenCalledWith(failure);
    expect(result.current.isSubmitting).toBe(false);
  });

  it('ignores a second submit while the first one is still in flight', async () => {
    const { result } = renderController();
    act(() => result.current.updateField('email', 'a@b.com'));
    act(() => result.current.updateField('name', 'Jane'));
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    let resolveOnValid: () => void = () => {};
    const onValid = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveOnValid = resolve;
        }),
    );

    act(() => {
      void result.current.submitForm({
        event: fakeEvent(),
        invalidMessage: 'invalid',
        onError: vi.fn(),
        onValid,
      });
    });
    await waitFor(() => expect(result.current.isSubmitting).toBe(true));

    await act(async () => {
      await result.current.submitForm({
        event: fakeEvent(),
        invalidMessage: 'invalid',
        onError: vi.fn(),
        onValid,
      });
    });

    expect(onValid).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveOnValid();
    });
  });
});
