package es.udc.fic.corpuslab.modules.project.guideline;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.guideline.dtos.ProjectGuidelinePdfContentDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;

@Service
public class ProjectGuidelineServiceImpl implements ProjectGuidelineService {

    private final AuthApiService authApiService;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final GuidelinePdfValidator guidelinePdfValidator;
    private final GuidelinePdfDecoder guidelinePdfDecoder;
    private final GuidelineFileNameGenerator guidelineFileNameGenerator;
    private final long maxGuidelinePdfSizeBytes;

    public ProjectGuidelineServiceImpl(
            AuthApiService authApiService,
            ProjectParticipantRepository projectParticipantRepository,
            GuidelinePdfValidator guidelinePdfValidator,
            GuidelinePdfDecoder guidelinePdfDecoder,
            GuidelineFileNameGenerator guidelineFileNameGenerator,
            @Value("${app.guideline.max-pdf-size-bytes:10485760}") long maxGuidelinePdfSizeBytes) {
        this.authApiService = authApiService;
        this.projectParticipantRepository = projectParticipantRepository;
        this.guidelinePdfValidator = guidelinePdfValidator;
        this.guidelinePdfDecoder = guidelinePdfDecoder;
        this.guidelineFileNameGenerator = guidelineFileNameGenerator;
        this.maxGuidelinePdfSizeBytes = Math.max(1L, maxGuidelinePdfSizeBytes);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectGuidelinePdfContentDto getProjectGuidelinePdf(String authenticatedEmail, Long projectId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        var participant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        byte[] guidelinePdfBytes = decodeGuidelinePdfBytes(
                participant.getProject().getGuideline() == null
                        ? null
                        : participant.getProject().getGuideline().getFileUrl());

        if (guidelinePdfBytes.length == 0) {
            throw new InvalidProjectSetupException("Project has no guideline PDF");
        }

        return new ProjectGuidelinePdfContentDto(
                buildGuidelinePdfFileName(participant.getProject()),
                "application/pdf",
                guidelinePdfBytes);
    }

    @Override
    public String normalizeAndValidateGuidelinePdfFile(MultipartFile guidelinePdfFile) {
        if (guidelinePdfFile == null || guidelinePdfFile.isEmpty()) {
            return null;
        }

        if (guidelinePdfFile.getSize() <= 0 || guidelinePdfFile.getSize() > maxGuidelinePdfSizeBytes) {
            throw new InvalidProjectSetupException("Guideline PDF exceeds the maximum allowed size");
        }

        byte[] pdfBytes;
        try {
            pdfBytes = guidelinePdfFile.getBytes();
        } catch (IOException ex) {
            throw new InvalidProjectSetupException("Guideline PDF could not be read");
        }

        guidelinePdfValidator.validate(pdfBytes, maxGuidelinePdfSizeBytes);

        return Base64.getEncoder().encodeToString(pdfBytes);
    }

    @Override
    public byte[] decodeGuidelinePdfBytes(String rawGuidelinePdfBase64) {
        return guidelinePdfDecoder.decode(rawGuidelinePdfBase64);
    }

    @Override
    public long calculateGuidelinePdfSizeBytes(String rawGuidelinePdfBase64) {
        return guidelinePdfDecoder.calculateSizeBytes(rawGuidelinePdfBase64);
    }

    @Override
    public String buildGuidelinePdfFileName(Project project) {
        return guidelineFileNameGenerator.build(project);
    }
}
