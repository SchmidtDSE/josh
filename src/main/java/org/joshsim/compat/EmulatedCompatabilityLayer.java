package org.joshsim.compat;

import java.math.BigDecimal;

public class EmulatedCompatabilityLayer implements CompatabilityLayer {

  private static final BigDecimal TWO = new BigDecimal("2");

  @Override
  public CompatibleStringJoiner createStringJoiner(String delimiter) {
    return new EmulatedStringJoiner(delimiter);
  }

  @Override
  public BigDecimal getTwo() {
    return TWO;
  }

  @Override
  public QueueService createQueueService(QueueServiceCallback callback) {
    return new EmulatedQueueService(callback);
  }

  @Override
  public CompatibleLock getLock() {
    return new EmulatedLock();
  }

}
