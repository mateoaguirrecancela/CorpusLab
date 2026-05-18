package es.udc.fic.corpuslab.modules.project.progress;

import java.util.Map;

public record ProjectProgressSnapshot(
        long totalSteps,
        int projectCompletionPercentage,
        Map<Long, Long> completedStepsByUser,
        Map<Long, Integer> completionPercentageByUser) {

    public long completedStepsForUser(Long userId) {
        return completedStepsByUser.getOrDefault(userId, 0L);
    }

    public int completionPercentageForUser(Long userId) {
        return completionPercentageByUser.getOrDefault(userId, 0);
    }
}
