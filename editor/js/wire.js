/**
 * Logic to serialize and deserialize internal data transfer formats.
 *
 * @license BSD-3-Clause
 */

import {OutputDatum, SimulationResultBuilder} from "model";


/**
 * Utility to serialize input data into the wire transfer format.
 *
 * Utility to serialize input data into the wire transfer format for both config and 
 * geospatial data, converting tabs within file contents into four spaces and sending 
 * binary data as base64 while including a flag indicating if the file is binary. This 
 * is used to run the virtual file system within the Josh server. In this format, each 
 * file is represented as a string with tab-separated values. The first value is the
 * name of the file (path in the virtual file system) followed by a non-spaces tab. 
 * The second value is either a 1 if the file's contents are base64 encoded binary data 
 * or a 0 if plain text followed by a non-spaces tab. Finally, the third value is the 
 * content of the file with tabs replaced with spaces followed by a tab. After the 
 * third tab, the next file starts or the string ends if no further files.
 */
class ExternalDataSerializer {

  /**
   * Convert from the JSON-serializable object with data files to the string wire transfer format.
   *
   * @param {Object} data - The JSON serializable object with data file information including
   *     both config (.jshc) and geospatial (.jshd) files. This object's attributes or keys 
   *     are the filenames or file paths. Meanwhile, this object's values are the contents 
   *     of files. For binary files, the contents of the files will be base64 encoded strings. 
   *     For all other files, the contents of the file are plain text strings.
   * @returns {string} String serialized encoding of these files in the wire format representing all
   *     files to use in the virtual file system.
   */
  serialize(data) {
    const self = this;
    return Object.entries(data).map((entry) => {
      const filename = entry[0];
      const content = entry[1];
      const isBinary = !self._isTextFile(filename);
      const safeContent = content.replace(/\t/g, '    ');
      return `${filename}\t${isBinary ? '1' : '0'}\t${safeContent}\t`;
    }).join('');
  }

  /**
   * Determine if a file is binary or plain text.
   *
   * @param {string} filename - The name of the file.
   * @returns {boolean} True if the file is a text file so its contents are plain text or False if
   *     the file is binary and its string contents are the base64 encoded version of its binary
   *     contents.
   */
  _isTextFile(filename) {
    const self = this;
    const extension = filename.substring(filename.lastIndexOf("."));
    
    switch (extension) {
      case ".csv":
      case ".txt":
      case ".jshc":
      case ".josh":
        return true;
      default:
        return false;
    }
  }
  
}


/**
 * Utility to parse responses from the engine.
 *
 * Utility to parse responses from the engine which uses an internal data transfer format. Each line
 * is a datum as described in parse.js which is shared across the main and worker javascript logic.
 */
class ResponseReader {

  /**
   * Create a new reader which parses external responses.
   *
   * @param {function} onReplicateExternal - Callback to invoke when replicates are ready.
   * @param {function} onStepExternal - Callback to invoke when step progress is reported.
   * @param {number} startStep - The starting step value (steps.low) for normalizing absolute
   *     timesteps to 0-based progress. Defaults to 0 if not provided.
   */
  constructor(onReplicateExternal, onStepExternal, startStep) {
    const self = this;
    self._replicateReducer = new Map();
    self._completeReplicates = [];
    self._onReplicateExternal = onReplicateExternal;
    self._onStepExternal = onStepExternal || (() => {});
    self._startStep = startStep || 0;
    self._buffer = "";
    self._completedReplicates = 0;
  }

  /**
   * Parse a response into OutputDatum SimulationResult objects.
   *
   * @param {string} text - The text returned by the engine in which the simulation is executing.
   */
  processResponse(text) {
    const self = this;

    self._buffer += text;
    const lines = self._buffer.split("\n");
    self._buffer = lines.pop();

    lines.map((x) => x.trim()).forEach((line) => {
      const intermediate = parseEngineResponse(line);
      if (intermediate === null) {
        return;
      }

      if (intermediate["type"] === "datum") {
        if (!self._replicateReducer.has(intermediate["replicate"])) {
          self._replicateReducer.set(intermediate["replicate"], new SimulationResultBuilder());
        }
        const rawInput = intermediate["datum"];
        const parsed = new OutputDatum(rawInput["target"], rawInput["attributes"]);
        self._replicateReducer.get(intermediate["replicate"]).add(parsed);
      } else if (intermediate["type"] === "end") {
        self._completedReplicates++;
        if (self._replicateReducer.has(intermediate["replicate"])) {
          self._completeReplicates.push(
            self._replicateReducer.get(intermediate["replicate"]).build()
          );
        }
        self._onReplicateExternal(self._completedReplicates);
      } else if (intermediate["type"] === "progress") {
        // Normalize absolute timestep to 0-based step count
        const absoluteStep = intermediate["steps"];
        const normalizedStep = absoluteStep - self._startStep;
        self._onStepExternal(normalizedStep);
      } else if (intermediate["type"] === "error") {
        throw new Error("Server error: " + intermediate["message"]);
      }
    });
  }

  /**
   * Get the buffer of response data not yet processed.
   *
   * @returns {string} Data waiting to be processed.
   */
  getBuffer() {
    const self = this;
    return self._buffer;
  }

  /**
   * Get a listing of all completed replicates.
   *
   * @returns {Array<SimulationResult>} Result from each replicate as an individual element in the
   *     resulting array.
   */
  getCompleteReplicates() {
    const self = this;
    return self._completeReplicates;
  }

}

export {ExternalDataSerializer, ResponseReader};
