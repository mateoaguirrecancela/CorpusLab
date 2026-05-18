package es.udc.fic.corpuslab.common.redis;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStreamService {

    private final StringRedisTemplate redisTemplate;

    public RedisStreamService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RecordId add(String streamKey, String group, Map<String, String> message) {
        RecordId recordId = redisTemplate.opsForStream().add(streamKey, message);
        ensureConsumerGroup(streamKey, group);
        return recordId;
    }

    public RecordId append(String streamKey, Map<String, String> message) {
        return redisTemplate.opsForStream().add(streamKey, message);
    }

    public long size(String streamKey) {
        Long size = redisTemplate.opsForStream().size(streamKey);
        return size == null ? 0L : size;
    }

    public long pending(String streamKey, String group) {
        if (!prepareStreamGroup(streamKey, group)) {
            return 0L;
        }

        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(streamKey, group);
        return summary == null ? 0L : summary.getTotalPendingMessages();
    }

    public long pending(String streamKey, String group, String consumerName) {
        if (!prepareStreamGroup(streamKey, group)) {
            return 0L;
        }

        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(streamKey, group);
        if (summary == null) {
            return 0L;
        }
        return summary.getPendingMessagesPerConsumer().getOrDefault(consumerName, 0L);
    }

    public long lag(String streamKey, String group) {
        if (!prepareStreamGroup(streamKey, group)) {
            return 0L;
        }

        StreamInfo.XInfoGroup groupInfo = groupInfo(streamKey, group);
        if (groupInfo == null) {
            return 0L;
        }
        Object lag = groupInfo.getRaw().get("lag");
        if (lag instanceof Number number) {
            return number.longValue();
        }
        if (lag == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(lag));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public long consumerCount(String streamKey, String group) {
        if (!prepareStreamGroup(streamKey, group)) {
            return 0L;
        }

        StreamInfo.XInfoConsumers consumers = redisTemplate.opsForStream().consumers(streamKey, group);
        return consumers == null ? 0L : consumers.getConsumerCount();
    }

    public long consumerIdleTimeSeconds(String streamKey, String group, String consumerName) {
        if (!prepareStreamGroup(streamKey, group)) {
            return 0L;
        }

        StreamInfo.XInfoConsumers consumers = redisTemplate.opsForStream().consumers(streamKey, group);
        if (consumers == null) {
            return 0L;
        }
        return consumers.stream()
                .filter(consumer -> consumerName.equals(consumer.consumerName()))
                .findFirst()
                .map(consumer -> consumer.idleTime().toSeconds())
                .orElse(0L);
    }

    public List<MapRecord<String, Object, Object>> readPendingThenNew(
            String streamKey,
            String group,
            String consumerName,
            long count) {
        if (!prepareStreamGroup(streamKey, group)) {
            return List.of();
        }

        List<MapRecord<String, Object, Object>> pendingRecords = readPending(streamKey, group, consumerName, count);

        if (pendingRecords != null && !pendingRecords.isEmpty()) {
            return pendingRecords;
        }

        return readNew(streamKey, group, consumerName, count);
    }

    public List<MapRecord<String, Object, Object>> readPending(
            String streamKey,
            String group,
            String consumerName,
            long count) {
        if (!prepareStreamGroup(streamKey, group)) {
            return List.of();
        }

        return redisTemplate.opsForStream().read(
                Consumer.from(group, consumerName),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamKey, ReadOffset.from("0-0")));
    }

    public List<MapRecord<String, Object, Object>> readNew(
            String streamKey,
            String group,
            String consumerName,
            long count) {
        if (!prepareStreamGroup(streamKey, group)) {
            return List.of();
        }

        return redisTemplate.opsForStream().read(
                Consumer.from(group, consumerName),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
    }

    public void acknowledge(String streamKey, String group, RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(streamKey, group, recordId);
    }

    private StreamInfo.XInfoGroup groupInfo(String streamKey, String group) {
        StreamInfo.XInfoGroups groups = redisTemplate.opsForStream().groups(streamKey);
        if (groups == null) {
            return null;
        }
        return groups.stream()
                .filter(groupInfo -> group.equals(groupInfo.groupName()))
                .findFirst()
                .orElse(null);
    }

    private void ensureConsumerGroup(String streamKey, String group) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), group);
        } catch (RuntimeException ex) {
            if (!isBusyGroup(ex)) {
                throw ex;
            }
        }
    }

    private boolean prepareStreamGroup(String streamKey, String group) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
            return false;
        }
        ensureConsumerGroup(streamKey, group);
        return true;
    }

    private boolean isBusyGroup(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
