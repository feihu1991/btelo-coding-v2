const fs = require('fs');
const { parseTranscriptEntries, deltaMessage } = require('./transcript-events');

class TranscriptTail {
  constructor({ filePath, relaySessionId, claudeSessionId, startSeq = 0, onDelta, onParsedEvent } = {}) {
    this.filePath = filePath;
    this.relaySessionId = relaySessionId;
    this.claudeSessionId = claudeSessionId;
    this.nextSeq = startSeq;
    this.onDelta = onDelta;
    this.onParsedEvent = onParsedEvent;
    this.lastSize = 0;
    this.partialLine = '';
    this.seenIds = new Set();
    this.watcher = null;
    this.debounce = null;
  }

  start() {
    if (!this.filePath || !fs.existsSync(this.filePath)) return false;
    this.stop();
    this.lastSize = fs.statSync(this.filePath).size;
    this.watcher = fs.watch(this.filePath, (eventType) => {
      if (eventType !== 'change') return;
      clearTimeout(this.debounce);
      this.debounce = setTimeout(() => this.readIncrement(), 75);
    });
    return true;
  }

  stop() {
    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
    if (this.debounce) {
      clearTimeout(this.debounce);
      this.debounce = null;
    }
  }

  readIncrement() {
    if (!this.filePath || !fs.existsSync(this.filePath)) return;

    const currentSize = fs.statSync(this.filePath).size;
    if (currentSize < this.lastSize) {
      this.lastSize = 0;
      this.partialLine = '';
      this.seenIds.clear();
    }
    if (currentSize <= this.lastSize) return;

    const fd = fs.openSync(this.filePath, 'r');
    const buffer = Buffer.alloc(currentSize - this.lastSize);
    fs.readSync(fd, buffer, 0, buffer.length, this.lastSize);
    fs.closeSync(fd);
    this.lastSize = currentSize;

    const chunk = this.partialLine + buffer.toString('utf-8');
    const lines = chunk.split('\n');
    this.partialLine = lines.pop() || '';

    const entries = [];
    for (const line of lines) {
      if (!line.trim()) continue;
      try { entries.push(JSON.parse(line)); } catch { /* wait for a valid future line */ }
    }
    if (entries.length === 0) return;

    const events = parseTranscriptEntries(entries, { startSeq: this.nextSeq })
      .filter((event) => {
        if (this.seenIds.has(event.id)) return false;
        this.seenIds.add(event.id);
        return true;
      });

    if (events.length === 0) return;
    this.nextSeq = events[events.length - 1].seq + 1;

    for (const event of events) {
      if (this.onParsedEvent) this.onParsedEvent(event);
    }

    if (this.onDelta) {
      this.onDelta(deltaMessage({
        relaySessionId: this.relaySessionId,
        claudeSessionId: this.claudeSessionId,
        events,
        cursor: this.nextSeq - 1
      }));
    }
  }
}

module.exports = TranscriptTail;
