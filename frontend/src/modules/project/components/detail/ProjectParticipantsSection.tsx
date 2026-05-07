import { PenSquare, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { RoleBadge } from '@/components/common/RoleBadge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { type ProjectDetail } from '@/modules/project/types/project';
import {
  getPersonInitials,
  participantRoleI18nKey,
} from '@/modules/project/utils/projectDisplayUtils';
import { normalizeCompletionPercentage } from '@/modules/project/utils/projectUtils';

type ProjectParticipantsSectionProps = Readonly<{
  project: ProjectDetail;
}>;

export function ProjectParticipantsSection({ project }: ProjectParticipantsSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
        <Users className="size-5" />
        {t('project.detail.participantsTitle')}
      </h2>

      {project.participants.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t('project.detail.participantsEmpty')}</p>
      ) : (
        <div className="rounded-md border border-border bg-surface-base">
          <Table>
            <TableHeader className="bg-muted/40">
              <TableRow className="hover:bg-transparent">
                <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  {t('project.detail.participantsName')}
                </TableHead>
                <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  {t('project.detail.participantsRole')}
                </TableHead>
                <TableHead className="px-4 py-3 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  {t('project.detail.participantsCompletion')}
                </TableHead>
                {project.participantRole === 'CREATOR' && (
                  <TableHead className="px-4 py-3 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.detail.participantsActions')}
                  </TableHead>
                )}
              </TableRow>
            </TableHeader>

            <TableBody>
              {project.participants.map((participant) => {
                const participantCompletion = normalizeCompletionPercentage(
                  participant.completionPercentage,
                );

                return (
                  <TableRow className="hover:bg-transparent" key={participant.userId}>
                    <TableCell className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="flex size-8 items-center justify-center rounded-full bg-accent text-xs font-bold text-primary">
                          {getPersonInitials(participant.firstName, participant.lastName)}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-primary">
                            {participant.firstName} {participant.lastName}
                          </p>
                          <p className="text-xs text-muted-foreground">{participant.email}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="px-4 py-3">
                      <RoleBadge
                        label={t(participantRoleI18nKey(participant.role))}
                        role={participant.role}
                      />
                    </TableCell>

                    <TableCell className="px-4 py-3 text-right text-sm font-semibold text-muted-foreground tabular-nums">
                      {participantCompletion}%
                    </TableCell>

                    {project.participantRole === 'CREATOR' && (
                      <TableCell className="px-4 py-3 text-right">
                        {participant.role === 'PARTICIPANT' ? (
                          <Link
                            className="inline-flex h-8 items-center gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground transition hover:bg-surface-soft hover:text-primary"
                            to={`/home/projects/${project.id}/annotate?participantUserId=${participant.userId}`}
                          >
                            <PenSquare className="size-3.5" />
                            {t('project.detail.viewParticipantAnnotations')}
                          </Link>
                        ) : (
                          <span className="text-xs text-muted-foreground">-</span>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}
