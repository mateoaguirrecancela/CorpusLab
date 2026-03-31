package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository memberRepository;

    public ResearchGroupServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        return memberRepository.findGroupSummariesByUserId(user.getId());
    }

    @Override
    @Transactional
    public ResearchGroupSummaryDto createResearchGroup(String authenticatedEmail, CreateResearchGroupRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        ResearchGroup group = new ResearchGroup();
        group.setName(request.name().trim());
        group.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null
        );
        group = researchGroupRepository.save(group);

        ResearchGroupMember ownerMember = new ResearchGroupMember();
        ownerMember.setUser(user);
        ownerMember.setResearchGroup(group);
        ownerMember.setRole(ResearchGroupMemberRole.OWNER);
        memberRepository.save(ownerMember);

        return new ResearchGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                ResearchGroupMemberRole.OWNER,
                1L,
                group.getCreatedAt()
        );
    }
}
