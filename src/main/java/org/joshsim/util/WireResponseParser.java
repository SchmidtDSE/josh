/**
 * Utility for parsing streaming responses from Josh remote servers.
 *
 * <p>This class provides functionality to parse wire format responses from remote Josh
 * servers, converting streaming response lines into structured data objects. It mirrors
 * the functionality of parseEngineResponse from parse.js in the JavaScript codebase.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for streaming response lines from remote Josh execution engines.
 *
 * <p>This utility class parses individual lines from streaming HTTP responses sent by
 * remote JoshSimServer instances. It supports various response types including data
 * points, progress updates, completion notifications, and error messages.</p>
 */
public class WireResponseParser {

  // Regex patterns for parsing different response types
  private static final Pattern END_PATTERN = Pattern.compile("^\\[end (\\d+)\\]$");
  private static final Pattern EMPTY_PATTERN = Pattern.compile("^\\[(\\d+)\\]$");
  private static final Pattern ERROR_PATTERN = Pattern.compile("^\\[error\\] (.+)$");
  private static final Pattern PROGRESS_PATTERN = Pattern.compile("^\\[progress (\\d+)\\]$");
  private static final Pattern DATUM_PATTERN = Pattern.compile("^\\[(\\d+)\\] (.+)$");

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

  /**
   * Container class for parsed response data.
   *
   * <p>This class encapsulates the different types of information that can be
   * contained in a streaming response line from a remote Josh server.</p>
   */
  public static class ParsedResponse {
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
      return String.format("ParsedResponse{type=%s, replicate=%d, step=%d, data='%s', error='%s'}",
          type, replicateNumber, stepCount, dataLine, errorMessage);
    }
  }

  /**
   * Parses a single response line from the remote engine.
   *
   * <p>This method analyzes a line from the streaming HTTP response and determines
   * its type and content. It supports the following formats:</p>
   * <ul>
   *   <li>[end N] - Replicate N has completed</li>
   *   <li>[progress N] - Current step is N</li>
   *   <li>[error] message - An error occurred with the given message</li>
   *   <li>[N] data - Data point from replicate N</li>
   *   <li>[N] - Empty data point from replicate N (ignored)</li>
   * </ul>
   *
   * @param line The response line to parse
   * @return A ParsedResponse object, or null if the line should be ignored
   * @throws IllegalArgumentException if the line format is invalid
   */
  public static ParsedResponse parseEngineResponse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return null;
    }

    String trimmed = line.trim();

    // Check for [end N] pattern
    Matcher endMatcher = END_PATTERN.matcher(trimmed);
    if (endMatcher.matches()) {
      int replicateNumber = Integer.parseInt(endMatcher.group(1));
      return new ParsedResponse(ResponseType.END, replicateNumber);
    }

    // Check for empty [N] pattern (should be ignored)
    Matcher emptyMatcher = EMPTY_PATTERN.matcher(trimmed);
    if (emptyMatcher.matches()) {
      return null;
    }

    // Check for [error] message pattern
    Matcher errorMatcher = ERROR_PATTERN.matcher(trimmed);
    if (errorMatcher.matches()) {
      String errorMessage = errorMatcher.group(1);
      return new ParsedResponse(errorMessage);
    }

    // Check for [progress N] pattern
    Matcher progressMatcher = PROGRESS_PATTERN.matcher(trimmed);
    if (progressMatcher.matches()) {
      long stepCount = Long.parseLong(progressMatcher.group(1));
      return new ParsedResponse(stepCount);
    }

    // Check for [N] data pattern
    Matcher datumMatcher = DATUM_PATTERN.matcher(trimmed);
    if (datumMatcher.matches()) {
      int replicateNumber = Integer.parseInt(datumMatcher.group(1));
      String dataLine = datumMatcher.group(2);
      
      if (dataLine == null || dataLine.trim().isEmpty()) {
        return null; // Ignore empty data
      }
      
      return new ParsedResponse(replicateNumber, dataLine);
    }

    // If no pattern matches, it's an error
    throw new IllegalArgumentException("Invalid engine response format: " + line);
  }
}