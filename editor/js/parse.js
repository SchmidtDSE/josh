/**
 * Logic to parse internal transfer strings.
 *
 * @license BSD-3-Clause
 */


/**
 * Parse a single data point from an internal transfer string without a replicate prefix.
 *
 * @param {string} source - The internal memory transfer string to parse. This should only contain
 *     one record.
 * @returns {Object} Record with the target name and attributes.
 */
function parseDatum(source) {
  const firstPieces = source.split(':', 2);
  const target = firstPieces[0];
  const attributesStr = firstPieces[1];

  const attributes = new Map();

  if (!attributesStr) {
    return;
  }

  const pairs = attributesStr.split("\t");
  for (const pair of pairs) {
    const pairPieces = pair.split('=', 2);
    const key = pairPieces[0];
    const value = pairPieces[1];

    const valid = key && value !== undefined;

    if (valid) {
      const isNumber = NUMBER_REGEX.test(value);
      attributes.set(key, isNumber ? parseFloat(value) : value);
    }
  }

  return {"target": target, "attributes": attributes};
}


/**
 * Parse a single data point from an internal transfer string with a replicate prefix.
 *
 * Parses a string returned from the engine such that the replicate number is provided in the
 * returned object along with a type attribute indicating what kind of message was recieved: either
 * a datum (in which case a datum attribute will have an object with target and attributes from
 * parseDatum) or a message indicating that the replicate has concluded.
 *
 * @param {string} source - The internal memory transfer string to parse. This should be a single
 *     line returned from the engine backend.
 * @returns {Object} Record with the replicate number and message type. If it contains a data point,
 *     it will also have a datum attribute.
 */
function parseEngineResponse(source) {
  
}
