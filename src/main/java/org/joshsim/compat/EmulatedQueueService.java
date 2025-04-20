
/**
 * A WebAssembly-compatible implementation of QueueService.
 *
 * <p>This implementation provides a simplified queue service for WebAssembly environments
 * where traditional JVM threading and queuing mechanisms are not available. It directly
 * forwards events to the callback handler without actual queuing.</p>
 */
package org.joshsim.compat;

import java.util.Optional;


public class EmulatedQueueService implements QueueService {

  private final QueueServiceCallback callback;

  /**
   * Creates a new EmulatedQueueService with the specified callback handler.
   *
   * @param callback The callback handler that will process queue events
   */
  public EmulatedQueueService(QueueServiceCallback callback) {
    this.callback = callback;
  }

  @Override
  public void start() {
    callback.onStart();
  }

  @Override
  public void join() {
    callback.onEnd();
  }

  @Override
  public void add(Object task) {
    callback.onTask(Optional.of(task));
  }
}
