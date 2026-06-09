import { type SetStateAction } from 'react';
import { create } from 'zustand';

type AnnotationUIState = {
  activeNerLabel: string;
  annotationOffset: number;
  hasResolvedResumeStep: boolean;
  isGuidelineCollapsed: boolean;
  resumeGlobalStepIndex: number | null;
  resetAnnotationUI: () => void;
  setActiveNerLabel: (nextValue: SetStateAction<string>) => void;
  setAnnotationOffset: (nextValue: SetStateAction<number>) => void;
  setHasResolvedResumeStep: (nextValue: SetStateAction<boolean>) => void;
  setIsGuidelineCollapsed: (nextValue: SetStateAction<boolean>) => void;
  setResumeGlobalStepIndex: (nextValue: SetStateAction<number | null>) => void;
};

function resolveStateAction<T>(nextValue: SetStateAction<T>, currentValue: T): T {
  return typeof nextValue === 'function'
    ? (nextValue as (previousValue: T) => T)(currentValue)
    : nextValue;
}

export const useAnnotationUIStore = create<AnnotationUIState>((set) => ({
  activeNerLabel: '',
  annotationOffset: 0,
  hasResolvedResumeStep: false,
  isGuidelineCollapsed: true,
  resumeGlobalStepIndex: null,
  resetAnnotationUI: () =>
    set({
      activeNerLabel: '',
      annotationOffset: 0,
      hasResolvedResumeStep: false,
      isGuidelineCollapsed: true,
      resumeGlobalStepIndex: null,
    }),
  setActiveNerLabel: (nextValue) =>
    set((state) => ({
      activeNerLabel: resolveStateAction(nextValue, state.activeNerLabel),
    })),
  setAnnotationOffset: (nextValue) =>
    set((state) => ({
      annotationOffset: resolveStateAction(nextValue, state.annotationOffset),
    })),
  setHasResolvedResumeStep: (nextValue) =>
    set((state) => ({
      hasResolvedResumeStep: resolveStateAction(nextValue, state.hasResolvedResumeStep),
    })),
  setIsGuidelineCollapsed: (nextValue) =>
    set((state) => ({
      isGuidelineCollapsed: resolveStateAction(nextValue, state.isGuidelineCollapsed),
    })),
  setResumeGlobalStepIndex: (nextValue) =>
    set((state) => ({
      resumeGlobalStepIndex: resolveStateAction(nextValue, state.resumeGlobalStepIndex),
    })),
}));
