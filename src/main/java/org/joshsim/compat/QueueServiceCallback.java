package org.joshsim.compat;


import java.util.Optional;

public interface QueueServiceCallback {

  void onStart();

  void onTask(Optional<Object> task);

  void onEnd();

}
