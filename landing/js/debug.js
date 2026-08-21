/**
 * Debug message classes for simulation debugging output.
 *
 * @license BSD-3-Clause
 */


/**
 * Represents a single debug message from the simulation.
 */
class DebugMessage {

  /**
   * Create a new debug message.
   *
   * @param {number} step - The simulation step when this message was emitted.
   * @param {string} entityType - The type of entity (organism, patch, etc.).
   * @param {string} entityId - The unique identifier of the entity.
   * @param {number} x - The x coordinate of the entity.
   * @param {number} y - The y coordinate of the entity.
   * @param {string} content - The actual debug message content.
   * @param {string} raw - The raw unparsed message string.
   */
  constructor(step, entityType, entityId, x, y, content, raw) {
    const self = this;
    self._step = step;
    self._entityType = entityType;
    self._entityId = entityId;
    self._x = x;
    self._y = y;
    self._content = content;
    self._raw = raw;
  }

  /**
   * Get the simulation step when this message was emitted.
   * @returns {number} The step number.
   */
  getStep() {
    return this._step;
  }

  /**
   * Get the entity type that generated this message.
   * @returns {string} The entity type (e.g., "organism", "patch").
   */
  getEntityType() {
    return this._entityType;
  }

  /**
   * Get the unique identifier of the entity.
   * @returns {string} The entity ID.
   */
  getEntityId() {
    return this._entityId;
  }

  /**
   * Get the x coordinate of the entity.
   * @returns {number} The x coordinate.
   */
  getX() {
    return this._x;
  }

  /**
   * Get the y coordinate of the entity.
   * @returns {number} The y coordinate.
   */
  getY() {
    return this._y;
  }

  /**
   * Get the debug message content.
   * @returns {string} The message content.
   */
  getContent() {
    return this._content;
  }

  /**
   * Get the raw unparsed message string.
   * @returns {string} The raw message.
   */
  getRaw() {
    return this._raw;
  }

  /**
   * Get a location key for filtering by patch.
   * @returns {string} Location key in format "x,y"
   */
  getLocationKey() {
    const self = this;
    return `${self._x},${self._y}`;
  }

  /**
   * Parse a debug message string into a DebugMessage object.
   *
   * Expected format: "[Step N, entityType @ entityId (x, y)] content"
   *
   * @param {string} raw - The raw debug message string.
   * @returns {?DebugMessage} Parsed message or null if parsing fails.
   */
  static parse(raw) {
    // Format: [Step N, entityType @ entityId (x, y)] content
    const match = raw.match(/^\[Step (\d+), (\w+) @ ([a-f0-9]+) \(([\d.]+), ([\d.]+)\)\] (.*)$/);
    if (!match) {
      console.warn("Failed to parse debug message:", raw);
      return null;
    }

    const step = parseInt(match[1], 10);
    const entityType = match[2];
    const entityId = match[3];
    const x = parseFloat(match[4]);
    const y = parseFloat(match[5]);
    const content = match[6];

    return new DebugMessage(step, entityType, entityId, x, y, content, raw);
  }
}


/**
 * Stores debug messages and provides filtering capabilities.
 */
class DebugMessageStore {

  constructor() {
    const self = this;
    self._messages = [];
    self._locations = new Set();
    self._steps = new Set();
    self._entityIds = new Set();
    self._entityTypes = new Set();
  }

  /**
   * Add a debug message to the store.
   * @param {DebugMessage} message - The message to add.
   */
  add(message) {
    const self = this;
    if (!message) return;

    self._messages.push(message);
    self._locations.add(message.getLocationKey());
    self._steps.add(message.getStep());
    self._entityIds.add(message.getEntityId());
    self._entityTypes.add(message.getEntityType());
  }

  /**
   * Clear all stored messages.
   */
  clear() {
    const self = this;
    self._messages = [];
    self._locations.clear();
    self._steps.clear();
    self._entityIds.clear();
    self._entityTypes.clear();
  }

  /**
   * Get all unique locations.
   * @returns {Array<string>} Sorted array of location keys.
   */
  getLocations() {
    const self = this;
    return Array.from(self._locations).sort();
  }

  /**
   * Get all unique steps.
   * @returns {Array<number>} Sorted array of step numbers.
   */
  getSteps() {
    const self = this;
    return Array.from(self._steps).sort((a, b) => a - b);
  }

  /**
   * Get all unique entity IDs.
   * @returns {Array<string>} Sorted array of entity IDs.
   */
  getEntityIds() {
    const self = this;
    return Array.from(self._entityIds).sort();
  }

  /**
   * Get all unique entity types.
   * @returns {Array<string>} Sorted array of entity types.
   */
  getEntityTypes() {
    const self = this;
    return Array.from(self._entityTypes).sort();
  }

  /**
   * Get total message count.
   * @returns {number} Total number of messages.
   */
  getCount() {
    const self = this;
    return self._messages.length;
  }

  /**
   * Get all messages.
   * @returns {Array<DebugMessage>} All stored messages.
   */
  getAll() {
    const self = this;
    return self._messages;
  }

  /**
   * Filter messages based on criteria.
   *
   * @param {?string} location - Location key to filter by, or null for all.
   * @param {?number} step - Step number to filter by, or null for all.
   * @param {?string} entityId - Entity ID to filter by, or null for all.
   * @param {?string} entityType - Entity type to filter by, or null for all.
   * @returns {Array<DebugMessage>} Filtered messages.
   */
  getFiltered(location, step, entityId, entityType) {
    const self = this;

    const getIsMatched = (filterVal, actual) => filterVal === null || filterVal === actual;

    return self._messages.filter((msg) => {
      let isMatched = getIsMatched(location, msg.getLocationKey());
      isMatched = isMatched && getIsMatched(step, msg.getStep());
      isMatched = isMatched && getIsMatched(entityId, msg.getEntityId());
      isMatched = isMatched && getIsMatched(entityType, msg.getEntityType());
      return isMatched;
    });
  }
}


export {DebugMessage, DebugMessageStore};
