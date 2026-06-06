/**
 * Represents the status of a batch job as returned by a polling strategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import java.util.Optional;


/**
 * Immutable snapshot of a batch job's current status.
 *
 * <p>Returned by {@link BatchPollingStrategy#poll(String)} to represent the
 * lifecycle state of a dispatched job. Includes optional metadata (message,
 * timestamp) when available from the underlying status source.</p>
 */
public class JobStatus {

  /**
   * Lifecycle states for a batch job.
   */
  public enum State {
    /** Job has been accepted but execution has not started. */
    PENDING,
    /** Job is currently executing. */
    RUNNING,
    /** Job completed successfully. */
    COMPLETE,
    /** Job failed with an error. */
    ERROR
  }

  private final State state;
  private final String message;
  private final String timestamp;

  /**
   * Constructs a JobStatus with all fields.
   *
   * @param state The current lifecycle state.
   * @param message Optional human-readable message (e.g., error details). May be null.
   * @param timestamp Optional ISO-8601 timestamp from the status source. May be null.
   */
  public JobStatus(State state, String message, String timestamp) {
    this.state = state;
    this.message = message;
    this.timestamp = timestamp;
  }

  /**
   * Constructs a JobStatus with only a state.
   *
   * @param state The current lifecycle state.
   */
  public JobStatus(State state) {
    this(state, null, null);
  }

  /**
   * Returns the current lifecycle state.
   *
   * @return The job state.
   */
  public State getState() {
    return state;
  }

  /**
   * Returns the optional human-readable message.
   *
   * @return The message, or empty if not available.
   */
  public Optional<String> getMessage() {
    return Optional.ofNullable(message);
  }

  /**
   * Returns the optional timestamp from the status source.
   *
   * @return The ISO-8601 timestamp, or empty if not available.
   */
  public Optional<String> getTimestamp() {
    return Optional.ofNullable(timestamp);
  }

  /**
   * Returns true if the job has reached a terminal state (COMPLETE or ERROR).
   *
   * @return True if the job is finished.
   */
  public boolean isTerminal() {
    return state == State.COMPLETE || state == State.ERROR;
  }
}
