package es.udc.fic.corpuslab.modules.researchgroup.api;

import java.util.List;
import java.util.Optional;

import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupInfo;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

/**
 * Public API contract for the researchgroup module.
 * Other modules MUST use this interface instead of importing
 * ResearchGroup/ResearchGroupMember entities or their repositories directly.
 */
public interface ResearchGroupApiService {

    /**
     * Finds a research group by ID.
     *
     * @return empty Optional if the group does not exist
     */
    Optional<ResearchGroupInfo> findGroupById(Long groupId);

    /**
     * Checks whether a research group exists.
     */
    boolean existsGroupById(Long groupId);

    /**
     * Finds the active membership of a user in a group.
     *
     * @return empty Optional if the user is not an active member
     */
    Optional<ResearchGroupMemberInfo> findActiveMember(Long groupId, Long userId);

    /**
     * Returns the user IDs of all active members in a group.
     */
    List<Long> findActiveMemberUserIds(Long groupId);
}
