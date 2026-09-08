package es.udc.fic.corpuslab.modules.researchgroup.api.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupInfo;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
@Transactional(readOnly = true)
public class ResearchGroupApiServiceImpl implements ResearchGroupApiService {

    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository memberRepository;

    public ResearchGroupApiServiceImpl(
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository memberRepository) {
        this.researchGroupRepository = researchGroupRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<ResearchGroupInfo> findGroupById(Long groupId) {
        return researchGroupRepository.findById(groupId)
                .map(this::toGroupInfo);
    }

    @Override
    public boolean existsGroupById(Long groupId) {
        return researchGroupRepository.existsById(groupId);
    }

    @Override
    public Optional<ResearchGroupMemberInfo> findActiveMember(Long groupId, Long userId) {
        return memberRepository.findActiveMemberByGroupIdAndUserId(groupId, userId)
                .map(this::toMemberInfo);
    }

    @Override
    public List<Long> findActiveMemberUserIds(Long groupId) {
        return memberRepository.findActiveMemberUserIdsByGroupId(groupId);
    }

    private ResearchGroupInfo toGroupInfo(ResearchGroup group) {
        return new ResearchGroupInfo(
                group.getId(),
                group.getName(),
                group.getDescription());
    }

    private ResearchGroupMemberInfo toMemberInfo(ResearchGroupMember member) {
        return new ResearchGroupMemberInfo(
                member.getUser().getId(),
                member.getRole().name());
    }
}
