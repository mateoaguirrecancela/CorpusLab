import { useCallback, useEffect, useMemo, useState } from 'react';
import { FlaskConical, MoreVertical, Plus, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { Button } from '@/components/ui/button';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Spinner } from '@/components/ui/spinner';
import {
  getResearchGroupDetail,
  getResearchGroupDetailErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import {
  type ResearchGroupDetail,
  type ResearchGroupMember,
} from '@/modules/researchgroup/types/researchGroup';

function getMemberInitials(member: ResearchGroupMember): string {
  const firstInitial = member.firstName.trim().charAt(0).toUpperCase();
  const lastInitial = member.lastName.trim().charAt(0).toUpperCase();

  return `${firstInitial}${lastInitial}`.trim();
}

function getRoleBadgeClasses(role: string): string {
  const normalizedRole = role.toUpperCase();
  if (normalizedRole === 'ADMIN' || normalizedRole === 'OWNER') {
    return 'bg-slate-100 text-slate-700';
  }
  return 'bg-indigo-50 text-indigo-700';
}

export default function ResearchGroupDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams();

  const [group, setGroup] = useState<ResearchGroupDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const numericGroupId = useMemo(() => Number(id), [id]);

  const loadGroupDetail = useCallback(async () => {
    if (!Number.isFinite(numericGroupId) || numericGroupId <= 0) {
      setErrorMessage(t('researchGroup.errors.invalidGroupId'));
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const data = await getResearchGroupDetail(numericGroupId);
      setGroup(data);
    } catch (error) {
      setErrorMessage(getResearchGroupDetailErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [numericGroupId, t]);

  useEffect(() => {
    void loadGroupDetail();
  }, [loadGroupDetail]);

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      {isLoading && (
        <div className="rounded-md border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('researchGroup.detail.loading')}
          </span>
        </div>
      )}

      {!isLoading && errorMessage.length > 0 && (
        <FeedbackMessage className="rounded-lg px-4 py-3" message={errorMessage} variant="error" />
      )}

      {!isLoading && !errorMessage && group && (
        <div className="space-y-7">
          <section className="rounded-md border border-[color:var(--cl-line)] bg-white p-6 sm:p-7">
            <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
              <div>
                <h1 className="text-4xl font-black tracking-tight text-[color:var(--cl-primary)]">
                  {group.name}
                </h1>
                {group.description && (
                  <p className="mt-2 max-w-3xl text-lg leading-tight text-[color:var(--cl-secondary)]">
                    {group.description}
                  </p>
                )}
              </div>

              <div className="flex lg:justify-end gap-4">
                <div className="rounded-md bg-background p-4 text-center w-30 h-30">
                  <p className="text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                    {t('researchGroup.detail.totalMembers')}
                  </p>
                  <p className="mt-2 text-4xl font-black leading-none text-[color:var(--cl-primary)]">
                    {group.totalMembers}
                  </p>
                </div>

                <div className="rounded-md bg-background p-4 text-center w-30 h-30">
                  <p className="text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                    {t('researchGroup.detail.activeProjects')}
                  </p>
                  <p className="mt-2 text-4xl font-black leading-none text-[color:var(--cl-primary)]">
                    {group.activeProjects}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="inline-flex items-center gap-4 text-3xl font-black tracking-tight text-[color:var(--cl-primary)]">
                <FlaskConical className="size-7" />
                {t('researchGroup.detail.experiments')}
              </h2>

              <Button
                className="h-10 rounded-md bg-[color:var(--cl-primary)] px-4 text-sm font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer"
                disabled
                type="button"
              >
                <Plus className="size-4" />
                {t('researchGroup.detail.newExperiment')}
              </Button>
            </div>

            <p className="rounded-md border border-dashed border-[color:var(--cl-line)] bg-white px-4 py-5 text-sm text-[color:var(--cl-secondary)]">
              {t('researchGroup.detail.experimentsDeferred')}
            </p>
          </section>

          <section>
            <div className="mb-5 flex items-center justify-between gap-3">
              <h2 className="inline-flex items-center gap-4 text-3xl font-black tracking-tight text-[color:var(--cl-primary)]">
                <Users className="size-7" />
                {t('researchGroup.detail.researchers')}
              </h2>

              <Button
                className="h-10 rounded-md bg-[color:var(--cl-primary)] px-4 text-sm font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer"
                type="button"
              >
                <Plus className="size-4" />
                {t('researchGroup.detail.inviteMember')}
              </Button>
            </div>

            <div className="overflow-x-auto rounded-md border border-[color:var(--cl-line)] bg-white">
              <table className="min-w-full border-collapse">
                <thead className="bg-[color:var(--sidebar)]">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                      {t('researchGroup.detail.columns.name')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                      {t('researchGroup.detail.columns.role')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                      {t('researchGroup.detail.columns.projects')}
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-bold tracking-widest text-[color:var(--cl-secondary)] uppercase">
                      {t('researchGroup.detail.columns.action')}
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {group.members.map((member) => (
                    <tr className="border-t border-[color:var(--cl-line)]" key={member.userId}>
                      <td className="px-6 py-4 align-middle">
                        <div className="flex items-center gap-3">
                          <div className="flex size-9 items-center justify-center rounded-full bg-[color:var(--cl-primary-soft)] text-xs font-bold text-[color:var(--cl-primary)]">
                            {getMemberInitials(member)}
                          </div>
                          <div>
                            <p className="text-base font-bold text-[color:var(--cl-primary)]">
                              {member.firstName} {member.lastName}
                            </p>
                            <p className="text-sm text-[color:var(--cl-secondary)]">
                              {member.email}
                            </p>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4 align-middle">
                        <span
                          className={`inline-flex rounded-sm px-2 py-1 text-xs font-semibold ${getRoleBadgeClasses(member.role)}`}
                        >
                          {t(`researchGroup.roles.${member.role}`)}
                        </span>
                      </td>

                      <td className="px-6 py-4 align-middle text-md font-bold text-[color:var(--cl-secondary)]">
                        00
                      </td>

                      <td className="px-6 py-4 text-right align-middle">
                        <button
                          aria-label={t('researchGroup.detail.memberActions')}
                          className="inline-flex size-8 items-center justify-center rounded-md text-[color:var(--cl-secondary)] transition-colors hover:bg-[color:var(--cl-primary-soft)] hover:text-[color:var(--cl-primary)]"
                          type="button"
                        >
                          <MoreVertical className="size-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}
