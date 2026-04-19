package es.udc.fic.corpuslab.modules.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationSchemaConstraintUpdater {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSchemaConstraintUpdater.class);

    private static final String UPDATE_NOTIFICATIONS_TYPE_CHECK_SQL = """
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name = 'notifications'
                ) THEN
                    ALTER TABLE notifications
                        DROP CONSTRAINT IF EXISTS notifications_type_check;

                    ALTER TABLE notifications
                        ADD CONSTRAINT notifications_type_check
                        CHECK (type IN (
                            'RESEARCH_GROUP_INVITATION_RECEIVED',
                            'RESEARCH_GROUP_INVITATION_ACCEPTED',
                            'PROJECT_PARTICIPANT_ASSIGNED',
                            'PROJECT_ANNOTATION_COMPLETED'
                        ));
                END IF;
            END;
            $$;
            """;

    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaConstraintUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void updateNotificationsTypeCheckConstraint() {
        try {
            jdbcTemplate.execute(UPDATE_NOTIFICATIONS_TYPE_CHECK_SQL);
        } catch (RuntimeException ex) {
            logger.warn("Could not refresh notifications_type_check constraint", ex);
        }
    }
}
