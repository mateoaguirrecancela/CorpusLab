package es.udc.fic.corpuslab.modules.project.guideline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;

class ProjectGuidelineServiceImplTest {

    private final ProjectGuidelineServiceImpl service = new ProjectGuidelineServiceImpl(
            null,
            null,
            new GuidelinePdfValidator(),
            new GuidelinePdfDecoder(),
            new GuidelineFileNameGenerator(),
            64L);

    @Test
    void normalizeAndValidateGuidelinePdfFileShouldReturnBase64ForValidPdf() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("guidelinePdf", "guide.pdf", "application/pdf", pdfBytes);

        String encoded = service.normalizeAndValidateGuidelinePdfFile(file);

        assertThat(service.decodeGuidelinePdfBytes(encoded)).isEqualTo(pdfBytes);
        assertThat(service.calculateGuidelinePdfSizeBytes(encoded)).isEqualTo(pdfBytes.length);
    }

    @Test
    void normalizeAndValidateGuidelinePdfFileShouldRejectNonPdfContent() {
        MockMultipartFile file = new MockMultipartFile(
                "guidelinePdf",
                "guide.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.normalizeAndValidateGuidelinePdfFile(file))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("valid PDF");
    }

    @Test
    void buildGuidelinePdfFileNameShouldSlugProjectName() {
        Project project = ProjectTestBuilder.validProject()
                .withName("  NER Demo: Phase 1  ")
                .build();

        assertThat(service.buildGuidelinePdfFileName(project)).isEqualTo("ner-demo-phase-1-guideline.pdf");
    }
}
