package org.joshsim.compat;

import java.util.Optional;


public class EmulatedQueueService implements QueueService {

  private final QueueServiceCallback callback;

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
