package es.udc.fic.corpuslab.modules.notification.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private EmailQueue emailQueue;

    @Mock
    private EmailTemplateRenderer templateRenderer;

    private EmailServiceImpl service() {
        return new EmailServiceImpl(emailQueue, templateRenderer, 15);
    }

    @Test
    void sendPasswordResetEmailShouldRenderTemplateAndEnqueueJob() {
        when(templateRenderer.render(eq("password-reset.html"), any())).thenReturn("<html>reset</html>");

        service().sendPasswordResetEmail("user@example.com", "https://corpuslab.test/reset");

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        verify(emailQueue).enqueue(jobCaptor.capture());
        EmailJob job = jobCaptor.getValue();
        assertThat(job.to()).isEqualTo("user@example.com");
        assertThat(job.subject()).isEqualTo("CorpusLab - Reset your password");
        assertThat(job.content()).isEqualTo("<html>reset</html>");

        verify(templateRenderer).render("password-reset.html", Map.of(
                "resetUrl", "https://corpuslab.test/reset",
                "expirationMinutes", 15L));
    }

    @Test
    void sendResearchGroupInvitationToExistingUserShouldRenderTemplateAndEnqueueJob() {
        when(templateRenderer.render(eq("research-group-invitation-existing-user.html"), any()))
                .thenReturn("<html>existing</html>");

        service().sendResearchGroupInvitationToExistingUser(
                "member@example.com", "Corpus Linguistics", "Alice", "https://corpuslab.test/invite/1");

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        verify(emailQueue).enqueue(jobCaptor.capture());
        EmailJob job = jobCaptor.getValue();
        assertThat(job.to()).isEqualTo("member@example.com");
        assertThat(job.subject()).isEqualTo("CorpusLab - Research group invitation");
        assertThat(job.content()).isEqualTo("<html>existing</html>");

        verify(templateRenderer).render("research-group-invitation-existing-user.html", Map.of(
                "groupName", "Corpus Linguistics",
                "inviterFullName", "Alice",
                "invitationUrl", "https://corpuslab.test/invite/1"));
    }

    @Test
    void sendResearchGroupInvitationToNewUserShouldRenderTemplateAndEnqueueJob() {
        when(templateRenderer.render(eq("research-group-invitation-new-user.html"), any()))
                .thenReturn("<html>new</html>");

        service().sendResearchGroupInvitationToNewUser(
                "newuser@example.com", "Corpus Linguistics", "Alice", "https://corpuslab.test/signup");

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        verify(emailQueue).enqueue(jobCaptor.capture());
        EmailJob job = jobCaptor.getValue();
        assertThat(job.to()).isEqualTo("newuser@example.com");
        assertThat(job.subject()).isEqualTo("CorpusLab - You were invited to a research group");
        assertThat(job.content()).isEqualTo("<html>new</html>");

        verify(templateRenderer).render("research-group-invitation-new-user.html", Map.of(
                "groupName", "Corpus Linguistics",
                "inviterFullName", "Alice",
                "signupUrl", "https://corpuslab.test/signup"));
    }

    @Test
    void sendProjectAssignmentEmailShouldRenderTemplateAndEnqueueJob() {
        when(templateRenderer.render(eq("project-assignment.html"), any())).thenReturn("<html>assignment</html>");

        service().sendProjectAssignmentEmail(
                "annotator@example.com", "NER Demo", "Alice", "https://corpuslab.test/projects/1");

        ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
        verify(emailQueue).enqueue(jobCaptor.capture());
        EmailJob job = jobCaptor.getValue();
        assertThat(job.to()).isEqualTo("annotator@example.com");
        assertThat(job.subject()).isEqualTo("CorpusLab - New project assignment");
        assertThat(job.content()).isEqualTo("<html>assignment</html>");

        verify(templateRenderer).render("project-assignment.html", Map.of(
                "projectName", "NER Demo",
                "assignerFullName", "Alice",
                "projectUrl", "https://corpuslab.test/projects/1"));
    }
}
