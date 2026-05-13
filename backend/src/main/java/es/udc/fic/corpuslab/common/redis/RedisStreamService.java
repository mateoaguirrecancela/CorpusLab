package es.udc.fic.corpuslab.common.redis;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
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

    public List<MapRecord<String, Object, Object>> readPendingThenNew(
            String streamKey,
            String group,
            String consumerName,
            long count) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
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
        return redisTemplate.opsForStream().read(
                Consumer.from(group, consumerName),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
    }

    public void acknowledge(String streamKey, String group, RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(streamKey, group, recordId);
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
