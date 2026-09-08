const NOTIFICATION_STREAM_EVENT = 'notification';
const NOTIFICATION_STREAM_DATA = 'changed';

type StreamBufferSplit = {
  events: string[];
  remainingBuffer: string;
};

export function buildNotificationStreamHeaders(
  token: string | null,
  language: string,
): HeadersInit {
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'Accept-Language': language,
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

export function splitNotificationStreamBuffer(buffer: string): StreamBufferSplit {
  const parts = buffer.split('\n\n');
  const remainingBuffer = parts.pop() ?? '';

  return {
    events: parts,
    remainingBuffer,
  };
}

export function hasNotificationChangeEvent(rawEvents: readonly string[]): boolean {
  return rawEvents.some(isNotificationChangeEvent);
}

function isNotificationChangeEvent(rawEvent: string): boolean {
  const { data, eventName } = parseServerSentEvent(rawEvent);

  return eventName === NOTIFICATION_STREAM_EVENT && data === NOTIFICATION_STREAM_DATA;
}

function parseServerSentEvent(rawEvent: string): { data: string; eventName: string } {
  const dataLines: string[] = [];
  let eventName = '';

  for (const rawLine of rawEvent.split('\n')) {
    const line = rawLine.trim();

    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim();
      continue;
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trim());
    }
  }

  return {
    data: dataLines.join('\n'),
    eventName,
  };
}
