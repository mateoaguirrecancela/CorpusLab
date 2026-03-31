package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private final UserRepository userRepository;
    private final ResearchGroupMemberRepository memberRepository;

    public ResearchGroupServiceImpl(
            UserRepository userRepository,
            ResearchGroupMemberRepository memberRepository) {
        this.userRepository = userRepository;
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
}
