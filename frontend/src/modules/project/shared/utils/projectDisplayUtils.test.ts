import { describe, expect, it } from 'vitest';
import {
  formatFileSize,
  getPersonInitials,
  isCsvFile,
  participantRoleI18nKey,
  projectTypeI18nKey,
} from '@/modules/project/shared/utils/projectDisplayUtils';

describe('projectTypeI18nKey', () => {
  it('maps every project type to its i18n key', () => {
    expect(projectTypeI18nKey('TEXT_CLASSIFICATION_SIMPLE')).toBe(
      'project.create.projectTypes.textClassificationSimple',
    );
    expect(projectTypeI18nKey('NER')).toBe('project.create.projectTypes.ner');
    expect(projectTypeI18nKey('SEQ2SEQ')).toBe('project.create.projectTypes.seq2seq');
  });
});

describe('participantRoleI18nKey', () => {
  it('maps CREATOR and falls back to participant otherwise', () => {
    expect(participantRoleI18nKey('CREATOR')).toBe('project.list.roles.creator');
    expect(participantRoleI18nKey('PARTICIPANT')).toBe('project.list.roles.participant');
  });
});

describe('getPersonInitials', () => {
  it('uppercases and trims the first letter of each name', () => {
    expect(getPersonInitials(' jane', 'doe ')).toBe('JD');
  });
});

describe('formatFileSize', () => {
  it('formats sub-kilobyte sizes as whole bytes', () => {
    expect(formatFileSize(500)).toBe('500 B');
  });

  it('floors negative or non-finite sizes to 0 bytes', () => {
    expect(formatFileSize(-10)).toBe('0 B');
    expect(formatFileSize(Number.NaN)).toBe('0 B');
    expect(formatFileSize(Number.POSITIVE_INFINITY)).toBe('0 B');
  });

  it('scales up through KB/MB/GB with one decimal', () => {
    expect(formatFileSize(1536)).toBe('1.5 KB');
    expect(formatFileSize(1024 * 1024 * 2)).toBe('2.0 MB');
  });

  it('caps at the largest unit (TB)', () => {
    expect(formatFileSize(1024 ** 5)).toBe('1024.0 TB');
  });
});

describe('isCsvFile', () => {
  it('delegates to the shared file utils', () => {
    expect(isCsvFile('text/csv', 'a.txt')).toBe(true);
    expect(isCsvFile('application/octet-stream', 'a.csv')).toBe(true);
    expect(isCsvFile('text/plain', 'a.txt')).toBe(false);
  });
});
