package es.udc.fic.corpuslab.modules.project.services;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository researchGroupMemberRepository;
    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository researchGroupMemberRepository,
            ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.researchGroupMemberRepository = researchGroupMemberRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId,
            CreateProjectRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(researchGroupId));

        ResearchGroupMember requesterMembership = researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this research group"));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && requesterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can create projects");
        }

        Project project = new Project();
        project.setResearchGroup(researchGroup);
        project.setName(request.name().trim());
        project.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);

        project = projectRepository.save(project);

        return new ProjectSummaryDto(
                project.getId(),
                researchGroup.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }
}
