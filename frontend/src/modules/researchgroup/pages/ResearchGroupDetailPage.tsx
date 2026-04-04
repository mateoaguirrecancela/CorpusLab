import { useEffect, useMemo } from 'react';
import { FilePenLine, FlaskConical, MoreVertical, Plus, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { EditResearchGroupDialog } from '@/modules/researchgroup/components/EditResearchGroupDialog';
import { InviteResearchGroupMemberDialog } from '@/modules/researchgroup/components/InviteResearchGroupMemberDialog';
import { ManageResearchGroupMemberDialog } from '@/modules/researchgroup/components/ManageResearchGroupMemberDialog';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getResearchGroupDetailErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupMember } from '@/modules/researchgroup/types/researchGroup';

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
  const { data: profile } = useProfileQuery();

  const numericGroupId = useMemo(() => Number(id), [id]);
  const isInvalidGroupId = !Number.isFinite(numericGroupId) || numericGroupId <= 0;
  const { data: group, isLoading, isError, error } = useResearchGroupDetailQuery(numericGroupId);
  const errorMessage = isInvalidGroupId
    ? t('researchGroup.errors.invalidGroupId')
    : isError
      ? getResearchGroupDetailErrorMessage(error)
      : '';

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'research-group-detail-load-error' });
    }
  }, [errorMessage]);

  const currentMember = useMemo(() => {
    if (!group || !profile?.email) {
      return undefined;
    }

    return group.members.find(
      (member) => member.email.toLowerCase() === profile.email.toLowerCase(),
    );
  }, [group, profile?.email]);

  const canManageResearchers = currentMember?.role === 'OWNER';

  return (
    <section className="px-5 py-4 sm:px-8 sm:py-4">
      {isLoading && (
        <div className="rounded-md border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('researchGroup.detail.loading')}
          </span>
        </div>
      )}

      {!isLoading && !errorMessage && group && (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-3">
            <BackButton fallbackTo="/home/research-groups" />

            {canManageResearchers && (
              <EditResearchGroupDialog
                groupId={group.id}
                initialDescription={group.description}
                initialName={group.name}
                trigger={
                  <Button
                    className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white px-4 text-sm font-semibold text-[color:var(--cl-primary)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
                    type="button"
                    variant="outline"
                  >
                    <FilePenLine className="size-4" />
                    {t('researchGroup.detail.editGroup')}
                  </Button>
                }
              />
            )}
          </div>

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

              <InviteResearchGroupMemberDialog
                groupId={group.id}
                invitationCode={group.invitationCode}
                trigger={
                  <Button
                    className="h-10 rounded-md bg-[color:var(--cl-primary)] px-4 text-sm font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer"
                    type="button"
                  >
                    <Plus className="size-4" />
                    {t('researchGroup.detail.inviteMember')}
                  </Button>
                }
              />
            </div>

            <div className="rounded-md border border-(--cl-line) bg-white">
              <Table>
                <TableHeader className="bg-muted/40">
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-(--cl-secondary) uppercase">
                      {t('researchGroup.detail.columns.name')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-(--cl-secondary) uppercase">
                      {t('researchGroup.detail.columns.role')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-(--cl-secondary) uppercase">
                      {t('researchGroup.detail.columns.projects')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-right text-xs font-semibold tracking-wide text-(--cl-secondary) uppercase">
                      {t('researchGroup.detail.columns.action')}
                    </TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {group.members.map((member) => (
                    <TableRow className="hover:bg-transparent" key={member.userId}>
                      <TableCell className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex size-8 items-center justify-center rounded-full bg-(--cl-primary-soft) text-xs font-bold text-(--cl-primary)">
                            {getMemberInitials(member)}
                          </div>
                          <div>
                            <p className="text-sm font-semibold text-(--cl-primary)">
                              {member.firstName} {member.lastName}
                            </p>
                            <p className="text-xs text-(--cl-secondary)">{member.email}</p>
                          </div>
                        </div>
                      </TableCell>

                      <TableCell className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-sm px-2 py-1 text-xs font-semibold ${getRoleBadgeClasses(member.role)}`}
                        >
                          {t(`researchGroup.roles.${member.role}`)}
                        </span>
                      </TableCell>

                      <TableCell className="px-4 py-3 text-sm font-semibold text-(--cl-secondary)">
                        00
                      </TableCell>

                      <TableCell className="px-4 py-3 text-right">
                        {canManageResearchers && member.role !== 'OWNER' && (
                          <ManageResearchGroupMemberDialog
                            groupId={group.id}
                            member={member}
                            trigger={
                              <button
                                aria-label={t('researchGroup.detail.memberActions')}
                                className="inline-flex size-8 items-center justify-center rounded-md text-[color:var(--cl-secondary)] transition-colors hover:bg-[color:var(--cl-primary-soft)] hover:text-[color:var(--cl-primary)] cursor-pointer"
                                type="button"
                              >
                                <MoreVertical className="size-4" />
                              </button>
                            }
                          />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}
