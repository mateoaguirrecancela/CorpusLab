package es.udc.fic.corpuslab.modules.project.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;

class ProjectProgressCalculatorTest {

    @Test
    void buildProjectProgressSnapshotShouldClampCountsAndCalculateProjectCompletion() {
        User first = userWithId(1L, "first@example.com");
        User second = userWithId(2L, "second@example.com");
        ProjectProgressCalculator calculator = new ProjectProgressCalculator(null, null, null);

        ProjectProgressSnapshot snapshot = calculator.buildProjectProgressSnapshot(
                List.of(participant(first), participant(second)),
                4L,
                Map.of(
                        1L, 5L,
                        2L, 2L));

        assertThat(snapshot.totalSteps()).isEqualTo(4L);
        assertThat(snapshot.completedStepsForUser(1L)).isEqualTo(4L);
        assertThat(snapshot.completedStepsForUser(2L)).isEqualTo(2L);
        assertThat(snapshot.completionPercentageForUser(1L)).isEqualTo(100);
        assertThat(snapshot.completionPercentageForUser(2L)).isEqualTo(50);
        assertThat(snapshot.projectCompletionPercentage()).isEqualTo(75);
    }

    @Test
    void buildProjectProgressSnapshotShouldSkipParticipantsWithoutUserOrUserId() {
        User validUser = userWithId(1L, "valid@example.com");
        User userWithoutId = UserTestBuilder.validUser().withEmail("no-id@example.com").build();
        ProjectProgressCalculator calculator = new ProjectProgressCalculator(null, null, null);

        ProjectProgressSnapshot snapshot = calculator.buildProjectProgressSnapshot(
                List.of(participant(validUser), participant(userWithoutId)),
                2L,
                Map.of(1L, 1L));

        assertThat(snapshot.completedStepsByUser()).containsOnlyKeys(1L);
    }

    private ProjectParticipant participant(User user) {
        return ProjectParticipantTestBuilder.validParticipant().withUser(user).build();
    }

    private User userWithId(Long id, String email) {
        User user = UserTestBuilder.validUser().withEmail(email).build();
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
