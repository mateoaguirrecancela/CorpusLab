import { useEffect, useMemo } from 'react';
import { FilePenLine, FlaskConical, MoreVertical, Plus, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { PageContainer } from '@/components/common/PageContainer';
import { RoleBadge } from '@/components/common/RoleBadge';
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
import { useAssignedProjectsByGroupQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';
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

export default function ResearchGroupDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: profile } = useProfileQuery();

  const numericGroupId = useMemo(() => Number(id), [id]);
  const isInvalidGroupId = !Number.isFinite(numericGroupId) || numericGroupId <= 0;
  const { data: group, isLoading, isError, error } = useResearchGroupDetailQuery(numericGroupId);
  let errorMessage = '';
  if (isInvalidGroupId) {
    errorMessage = t('researchGroup.errors.invalidGroupId');
  } else if (isError) {
    errorMessage = getResearchGroupDetailErrorMessage(error);
  }

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'research-group-detail-load-error' });
    }
  }, [errorMessage]);

  const profileEmail = profile?.email?.toLowerCase();
  const currentMember =
    group && profileEmail
      ? group.members.find((member) => member.email.toLowerCase() === profileEmail)
      : undefined;

  const canManageResearchers = currentMember?.role === 'OWNER';
  const canCreateProjects = currentMember?.role === 'OWNER' || currentMember?.role === 'ADMIN';
  const {
    data: assignedProjects = [],
    isLoading: isLoadingProjects,
    isError: isProjectsError,
    error: projectsError,
  } = useAssignedProjectsByGroupQuery(numericGroupId);

  const projectsErrorMessage = isProjectsError ? getProjectsLoadErrorMessage(projectsError) : '';

  useEffect(() => {
    if (projectsErrorMessage.length > 0) {
      toast.error(projectsErrorMessage, { id: 'group-projects-load-error' });
    }
  }, [projectsErrorMessage]);

  return (
    <PageContainer>
      {isLoading && (
        <div className="text-sm text-muted-foreground">
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
                onDeleted={() => navigate('/home/research-groups')}
                showDeleteButton
                trigger={
                  <Button
                    className="h-10 rounded-md border border-border bg-surface-base px-4 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
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

          <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
            <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
              <div>
                <h1 className="text-4xl font-black tracking-tight text-primary">{group.name}</h1>
                {group.description && (
                  <p className="mt-2 max-w-3xl text-lg leading-tight text-muted-foreground">
                    {group.description}
                  </p>
                )}
              </div>

              <div className="flex lg:justify-end gap-4">
                <div className="rounded-md bg-background p-4 text-center w-30 h-30">
                  <p className="text-xs font-bold tracking-widest text-muted-foreground uppercase">
                    {t('researchGroup.detail.totalMembers')}
                  </p>
                  <p className="mt-2 text-4xl font-black leading-none text-primary">
                    {group.totalMembers}
                  </p>
                </div>

                <div className="rounded-md bg-background p-4 text-center w-30 h-30">
                  <p className="text-xs font-bold tracking-widest text-muted-foreground uppercase">
                    {t('researchGroup.detail.activeProjects')}
                  </p>
                  <p className="mt-2 text-4xl font-black leading-none text-primary">
                    {group.activeProjects}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="inline-flex items-center gap-4 text-3xl font-black tracking-tight text-primary">
                <FlaskConical className="size-7" />
                {t('researchGroup.detail.projects')}
              </h2>

              {canCreateProjects && (
                <Button
                  className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
                  onClick={() => navigate(`/home/projects/create?groupId=${group.id}`)}
                  type="button"
                >
                  <Plus className="size-4" />
                  {t('researchGroup.detail.newProject')}
                </Button>
              )}
            </div>

            {isLoadingProjects && (
              <div className="rounded-md border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
                <span className="inline-flex items-center gap-2">
                  <Spinner aria-hidden className="size-4" />
                  {t('project.list.loading')}
                </span>
              </div>
            )}

            {!isLoadingProjects && assignedProjects.length === 0 && (
              <p className="rounded-md border border-dashed border-border bg-surface-base px-4 py-5 text-sm text-muted-foreground">
                {t('project.list.empty')}
              </p>
            )}

            {!isLoadingProjects && assignedProjects.length > 0 && (
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {assignedProjects.map((project) => {
                  const roleLabel =
                    project.participantRole === 'CREATOR'
                      ? t('project.list.roles.creator')
                      : t('project.list.roles.participant');

                  return (
                    <EntitySummaryCard
                      actionLabel={t('project.list.openProject')}
                      completionPercentage={project.completionPercentage}
                      description={project.description}
                      footerMeta={{
                        kind: 'research-group',
                        text: project.researchGroupName,
                      }}
                      key={project.id}
                      onAction={() => navigate(`/home/projects/${project.id}`)}
                      roleLabel={roleLabel}
                      role={project.participantRole}
                      title={project.name}
                    />
                  );
                })}
              </div>
            )}
          </section>

          <section>
            <div className="mb-5 flex items-center justify-between gap-3">
              <h2 className="inline-flex items-center gap-4 text-3xl font-black tracking-tight text-primary">
                <Users className="size-7" />
                {t('researchGroup.detail.researchers')}
              </h2>

              <InviteResearchGroupMemberDialog
                groupId={group.id}
                invitationCode={group.invitationCode}
                trigger={
                  <Button
                    className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
                    type="button"
                  >
                    <Plus className="size-4" />
                    {t('researchGroup.detail.inviteMember')}
                  </Button>
                }
              />
            </div>

            <div className="rounded-md border border-border bg-surface-base">
              <Table>
                <TableHeader className="bg-muted/40">
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                      {t('researchGroup.detail.columns.name')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                      {t('researchGroup.detail.columns.role')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                      {t('researchGroup.detail.columns.projects')}
                    </TableHead>
                    <TableHead className="px-4 py-3 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                      {t('researchGroup.detail.columns.action')}
                    </TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {group.members.map((member) => (
                    <TableRow className="hover:bg-transparent" key={member.userId}>
                      <TableCell className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex size-8 items-center justify-center rounded-full bg-accent text-xs font-bold text-primary">
                            {getMemberInitials(member)}
                          </div>
                          <div>
                            <p className="text-sm font-semibold text-primary">
                              {member.firstName} {member.lastName}
                            </p>
                            <p className="text-xs text-muted-foreground">{member.email}</p>
                          </div>
                        </div>
                      </TableCell>

                      <TableCell className="px-4 py-3">
                        <RoleBadge
                          label={t(`researchGroup.roles.${member.role}`)}
                          role={member.role}
                        />
                      </TableCell>

                      <TableCell className="px-4 py-3 text-sm font-semibold text-muted-foreground tabular-nums">
                        {String(member.activeProjectsCount).padStart(2, '0')}
                      </TableCell>

                      <TableCell className="px-4 py-3 text-right">
                        {canManageResearchers && member.role !== 'OWNER' && (
                          <ManageResearchGroupMemberDialog
                            groupId={group.id}
                            member={member}
                            trigger={
                              <button
                                aria-label={t('researchGroup.detail.memberActions')}
                                className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-primary cursor-pointer"
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
    </PageContainer>
  );
}
