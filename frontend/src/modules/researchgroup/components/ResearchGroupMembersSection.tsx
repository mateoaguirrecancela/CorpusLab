import { MoreVertical, Plus, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { RoleBadge } from '@/components/common/RoleBadge';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { InviteResearchGroupMemberDialog } from '@/modules/researchgroup/components/InviteResearchGroupMemberDialog';
import { ManageResearchGroupMemberDialog } from '@/modules/researchgroup/components/ManageResearchGroupMemberDialog';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';
import { getResearchGroupMemberInitials } from '@/modules/researchgroup/utils/researchGroupUtils';

type ResearchGroupMembersSectionProps = Readonly<{
  canManageResearchers: boolean;
  group: ResearchGroupDetail;
}>;

export function ResearchGroupMembersSection({
  canManageResearchers,
  group,
}: ResearchGroupMembersSectionProps) {
  const { t } = useTranslation();

  return (
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
                      {getResearchGroupMemberInitials(member)}
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
                  <RoleBadge label={t(`researchGroup.roles.${member.role}`)} role={member.role} />
                </TableCell>

                <TableCell className="px-4 py-3 text-sm font-semibold text-muted-foreground tabular-nums">
                  {member.activeProjectsCount}
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
  );
}
