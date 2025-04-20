package org.joshsim.compat;

public interface QueueService {

  void start();

  void join();

  void add(Object task);

}
