package es.udc.fic.corpuslab.modules.notification.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Year;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void renderShouldApplyLayoutAndEscapeVariables() {
        String html = renderer.render(
                "project-assignment.html",
                Map.of(
                        "assignerFullName", "Alice <script>",
                        "projectName", "Corpus \"Alpha\"",
                        "projectUrl", "https://example.com/?q=<unsafe>"));

        assertThat(html).contains("CorpusLab");
        assertThat(html).contains("Alice &lt;script&gt;");
        assertThat(html).contains("Corpus &quot;Alpha&quot;");
        assertThat(html).contains("https://example.com/?q=&lt;unsafe&gt;");
        assertThat(html).contains("&copy; " + Year.now() + " CorpusLab");
    }

    @Test
    void renderShouldFailWhenTemplateDoesNotExist() {
        assertThatThrownBy(() -> renderer.render("missing-template.html", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read email template");
    }
}
