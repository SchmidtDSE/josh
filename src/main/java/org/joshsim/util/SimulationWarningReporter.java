package org.joshsim.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Emits deduplicated, run-scoped simulation warnings. */
public final class SimulationWarningReporter {

  private static final SimulationWarningReporter NO_OP = new SimulationWarningReporter(null);

  private final OutputOptions output;
  private final Set<String> emittedKeys = ConcurrentHashMap.newKeySet();

  private SimulationWarningReporter(OutputOptions output) {
    this.output = output;
  }

  /** Creates a reporter that routes warnings through the supplied command output. */
  public static SimulationWarningReporter forOutput(OutputOptions output) {
    return new SimulationWarningReporter(output);
  }

  /** Gets a reporter that intentionally suppresses all warnings. */
  public static SimulationWarningReporter noOp() {
    return NO_OP;
  }

  /** Emits a warning at most once for the supplied run-local key. */
  public void warnOnce(String key, String message) {
    if (output != null && emittedKeys.add(key)) {
      output.printWarning(message);
    }
  }
}
