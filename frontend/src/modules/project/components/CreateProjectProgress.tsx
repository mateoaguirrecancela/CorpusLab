type CreateProjectProgressProps = Readonly<{
  currentStep: number;
  totalSteps?: number;
}>;

export function CreateProjectProgress({ currentStep, totalSteps = 4 }: CreateProjectProgressProps) {
  const progressPercentage = ((currentStep - 1) / (totalSteps - 1)) * 100;

  return (
    <div className="relative mb-6">
      <div className="absolute top-1/2 left-0 right-0 h-px -translate-y-1/2 bg-border" />
      <div
        className="absolute top-1/2 left-0 h-px -translate-y-1/2 bg-primary transition-all duration-500"
        style={{ width: `${progressPercentage}%` }}
      />

      <div className="relative flex items-center justify-between">
        {Array.from({ length: totalSteps }, (_, index) => index + 1).map((step) => {
          const isCompleted = step <= currentStep;

          return (
            <div
              className={[
                'size-4 rounded-full border-2 transition-colors duration-500 z-10',
                isCompleted ? 'border-primary bg-primary' : 'border-border bg-surface-base',
              ].join(' ')}
              key={step}
            />
          );
        })}
      </div>
    </div>
  );
}
