/**
 * Container class for parsed response data from wire format responses.
 *
 * <p>This class encapsulates the different types of information that can be
 * contained in a streaming response line from a remote Josh server.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

/**
 * Container class for parsed response data.
 *
 * <p>This class encapsulates the different types of information that can be
 * contained in a streaming response line from a remote Josh server.</p>
 */
public class ParsedResponse {

  /**
   * Enumeration of possible response types from the remote engine.
   */
  public enum ResponseType {
    /** A data point containing simulation results. */
    DATUM,
    /** Progress update indicating current step number. */
    PROGRESS,
    /** End notification indicating replicate completion. */
    END,
    /** Error message from remote execution. */
    ERROR
  }

  private final ResponseType type;
  private final int replicateNumber;
  private final String dataLine;
  private final long stepCount;
  private final String errorMessage;

  /**
   * Constructor for DATUM responses.
   *
   * @param replicateNumber The replicate number this data belongs to
   * @param dataLine The wire format data line
   */
  public ParsedResponse(int replicateNumber, String dataLine) {
    this.type = ResponseType.DATUM;
    this.replicateNumber = replicateNumber;
    this.dataLine = dataLine;
    this.stepCount = 0;
    this.errorMessage = null;
  }

  /**
   * Constructor for PROGRESS responses.
   *
   * @param stepCount The current step number
   */
  public ParsedResponse(long stepCount) {
    this.type = ResponseType.PROGRESS;
    this.replicateNumber = -1;
    this.dataLine = null;
    this.stepCount = stepCount;
    this.errorMessage = null;
  }

  /**
   * Constructor for END responses.
   *
   * @param type The response type (must be END)
   * @param replicateNumber The replicate number that completed
   */
  public ParsedResponse(ResponseType type, int replicateNumber) {
    if (type != ResponseType.END) {
      throw new IllegalArgumentException("This constructor is only for END responses");
    }
    this.type = type;
    this.replicateNumber = replicateNumber;
    this.dataLine = null;
    this.stepCount = 0;
    this.errorMessage = null;
  }

  /**
   * Constructor for ERROR responses.
   *
   * @param errorMessage The error message from the remote server
   */
  public ParsedResponse(String errorMessage) {
    this.type = ResponseType.ERROR;
    this.replicateNumber = -1;
    this.dataLine = null;
    this.stepCount = 0;
    this.errorMessage = errorMessage;
  }

  /**
   * Gets the response type.
   *
   * @return The type of this response
   */
  public ResponseType getType() {
    return type;
  }

  /**
   * Gets the replicate number.
   *
   * @return The replicate number, or -1 if not applicable
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }

  /**
   * Gets the data line for DATUM responses.
   *
   * @return The wire format data line, or null if not a DATUM response
   */
  public String getDataLine() {
    return dataLine;
  }

  /**
   * Gets the step count for PROGRESS responses.
   *
   * @return The step count, or 0 if not a PROGRESS response
   */
  public long getStepCount() {
    return stepCount;
  }

  /**
   * Gets the error message for ERROR responses.
   *
   * @return The error message, or null if not an ERROR response
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return String.format(
        "ParsedResponse{type=%s, replicate=%d, step=%d, data='%s', error='%s'}",
        type,
        replicateNumber,
        stepCount,
        dataLine,
        errorMessage);
  }
}
