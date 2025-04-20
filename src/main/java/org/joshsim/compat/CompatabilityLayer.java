package org.joshsim.compat;

import java.math.BigDecimal;

public interface CompatabilityLayer {

  CompatibleStringJoiner createStringJoiner(String delimiter);

  BigDecimal getTwo();

  QueueService createQueueService(QueueServiceCallback callback);

  CompatibleLock getLock();

}
