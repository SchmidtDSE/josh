package org.joshsim.compat;

import java.math.BigDecimal;


public class JvmCompatibilityLayer implements CompatabilityLayer {

  @Override
  public CompatibleStringJoiner createStringJoiner(String delimiter) {
    return new JvmStringJoiner(delimiter);
  }

  @Override
  public BigDecimal getTwo() {
    return BigDecimal.TWO;
  }

  @Override
  public QueueService createQueueService(QueueServiceCallback callback) {
    return new JvmQueueService(callback);
  }

  @Override
  public CompatibleLock getLock() {
    return new JvmLock();
  }

}
