import { useEffect } from 'react';
import { toast } from 'sonner';

type UseToastMessagesOptions = {
  errorMessage?: string;
  successMessage?: string;
  errorToastId?: string;
};

export function useToastMessages({
  errorMessage,
  successMessage,
  errorToastId,
}: UseToastMessagesOptions): void {
  useEffect(() => {
    if ((errorMessage ?? '').trim().length > 0) {
      if (errorToastId) {
        toast.error(errorMessage, { id: errorToastId });
        return;
      }

      toast.error(errorMessage);
    }
  }, [errorMessage, errorToastId]);

  useEffect(() => {
    if ((successMessage ?? '').trim().length > 0) {
      toast.success(successMessage);
    }
  }, [successMessage]);
}
