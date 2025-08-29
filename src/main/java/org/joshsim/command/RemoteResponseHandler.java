/**
 * Shared handler for processing remote simulation responses.
 *
 * <p>This class consolidates the duplicate response handling logic that was previously
 * spread across RunRemoteOffloadLeaderStrategy and RunRemoteLocalLeaderStrategy. It
 * provides a unified way to process wire format responses, manage export facades,
 * and track progress across different remote execution strategies.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.ExportFacadeFactory;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.util.ProgressUpdate;
import org.joshsim.wire.NamedMap;
import org.joshsim.wire.WireConverter;
import org.joshsim.wire.WireResponse;
import org.joshsim.wire.WireResponseParser;
import org.joshsim.wire.WireRewriteUtil;

/**
 * Shared handler for processing remote simulation responses.
 *
 * <p>This class provides unified response processing for both remote leader and local leader
 * execution strategies. It manages wire format parsing, data persistence via export facades,
 * and progress reporting with support for both individual and cumulative progress tracking.</p>
 */
public class RemoteResponseHandler {

  private final RunRemoteContext context;
  private final ExportFacadeFactory exportFactory;
  private final Map<String, ExportFacade> exportFacades;
  private final AtomicLong currentStep;
  private final AtomicInteger completedReplicates;
  private final boolean useCumulativeProgress;
  private int lastProcessedReplicate = -1;

  /**
   * Creates a new RemoteResponseHandler.
   *
   * @param context The execution context containing progress calculator and output options
   * @param exportFactory Factory for creating export facades
   * @param useCumulativeProgress Whether to use cumulative progress tracking for coordination
   */
  public RemoteResponseHandler(RunRemoteContext context,
      ExportFacadeFactory exportFactory,
      boolean useCumulativeProgress) {
    this.context = context;
    this.exportFactory = exportFactory;
    this.exportFacades = new HashMap<>();
    this.currentStep = new AtomicLong(0);
    this.completedReplicates = new AtomicInteger(0);
    this.useCumulativeProgress = useCumulativeProgress;
  }

  /**
   * Processes a line of streaming response from a remote server or worker.
   *
   * <p>This method provides centralized handling for all wire format response types:
   * DATUM, PROGRESS, END, and ERROR. It manages data persistence, progress tracking,
   * and error handling in a consistent manner across different execution strategies.</p>
   *
   * @param line The response line from the remote server or worker
   * @param replicateNumber The replicate number for this response (for error reporting)
   * @param cumulativeStepCount Optional cumulative step counter for progress coordination
   * @return The parsed WireResponse for further processing by the caller, or empty if ignored
   */
  public Optional<WireResponse> processResponseLine(String line, int replicateNumber,
      AtomicInteger cumulativeStepCount) {
    try {
      return parseResponseLineUnsafe(line, replicateNumber, cumulativeStepCount);
    } catch (Exception e) {
      throw new RuntimeException("Failed to process response for replicate "
          + replicateNumber + ": " + e.getMessage(), e);
    }
  }

  /**
   * Parses a response line without exception handling.
   *
   * <p>This method contains the core parsing logic without try/catch handling,
   * allowing the calling method to handle exceptions appropriately.</p>
   *
   * @param line The response line from the remote server or worker
   * @param replicateNumber The replicate number for this response (for error reporting)
   * @param cumulativeStepCount Optional cumulative step counter for progress coordination
   * @return The parsed WireResponse for further processing by the caller, or empty if ignored
   */
  private Optional<WireResponse> parseResponseLineUnsafe(String line, int replicateNumber,
      AtomicInteger cumulativeStepCount) {
    // Parse line using WireResponseParser
    Optional<WireResponse> optionalParsed =
        WireResponseParser.parseEngineResponse(line.trim());

    if (!optionalParsed.isPresent()) {
      return Optional.empty(); // Skip ignored lines
    }

    WireResponse parsed = optionalParsed.get();

    switch (parsed.getType()) {
      case DATUM -> {
        handleDatumResponse(parsed);
      }

      case PROGRESS -> {
        handleProgressResponse(parsed, cumulativeStepCount);
      }

      case END -> {
        handleEndResponse();
      }

      case ERROR -> {
        throw new RuntimeException("Remote execution error for replicate "
            + replicateNumber + ": " + parsed.getErrorMessage());
      }

      default -> {
        throw new IllegalArgumentException("Unknown wire response type: " + parsed.getType());
      }
    }

    return optionalParsed;
  }

  /**
   * Handles DATUM response by deserializing to NamedMap and persisting via export facade.
   *
   * @param response The DATUM response to process
   */
  private void handleDatumResponse(WireResponse response) {
    // Deserialize wire format to NamedMap using Component 1
    NamedMap namedMap = WireConverter.deserializeFromString(response.getDataLine());

    // Get or create export facade for this entity type
    String entityName = namedMap.getName();
    ExportFacade exportFacade = exportFacades.get(entityName);
    if (exportFacade == null) {
      // Create export target for CSV output
      ExportTarget target = new ExportTarget("file", entityName + ".csv");
      exportFacade = exportFactory.build(target);
      exportFacade.start();
      exportFacades.put(entityName, exportFacade);
    }

    // Persist using Component 2 NamedMap write capability
    exportFacade.write(namedMap, currentStep.get());
  }

  /**
   * Handles PROGRESS response with optional cumulative progress tracking.
   *
   * @param response The PROGRESS response to process
   * @param cumulativeStepCount Optional cumulative counter for coordination
   */
  private void handleProgressResponse(WireResponse response, AtomicInteger cumulativeStepCount) {
    currentStep.set(response.getStepCount());

    long stepCountToReport = response.getStepCount();

    if (useCumulativeProgress && cumulativeStepCount != null) {
      // Use cumulative progress for coordinated reporting
      WireResponse cumulativeProgress = WireRewriteUtil.rewriteProgressToCumulative(
          response,
          cumulativeStepCount
      );
      stepCountToReport = cumulativeProgress.getStepCount();
    }

    ProgressUpdate progressUpdate = context.getProgressCalculator()
        .updateStep(stepCountToReport);
    if (progressUpdate.shouldReport()) {
      context.getOutputOptions().printInfo(progressUpdate.getMessage());
    }
  }

  /**
   * Handles END response by updating completed replicate count and progress.
   */
  private void handleEndResponse() {
    int completedCount = completedReplicates.incrementAndGet();
    ProgressUpdate endUpdate = context.getProgressCalculator()
        .updateReplicateCompleted(completedCount);
    context.getOutputOptions().printInfo(endUpdate.getMessage());

    // Prepare for next replicate if there are more replicates to process
    if (completedCount < context.getReplicates()) {
      context.getProgressCalculator().resetForNextReplicate(completedCount + 1);
    }
  }

  /**
   * Gets the map of active export facades.
   *
   * @return Map of entity name to export facade
   */
  public Map<String, ExportFacade> getExportFacades() {
    return exportFacades;
  }

  /**
   * Gets the current step counter.
   *
   * @return The current step counter
   */
  public AtomicLong getCurrentStep() {
    return currentStep;
  }

  /**
   * Gets the completed replicates counter.
   *
   * @return The completed replicates counter
   */
  public AtomicInteger getCompletedReplicates() {
    return completedReplicates;
  }

  /**
   * Closes all export facades properly.
   *
   * <p>This method should be called in a finally block to ensure all export facades
   * are properly closed even if an exception occurs during processing.</p>
   */
  public void closeExportFacades() {
    for (ExportFacade facade : exportFacades.values()) {
      try {
        facade.join();
      } catch (Exception e) {
        context.getOutputOptions().printError("Failed to close export facade: "
            + e.getMessage());
      }
    }
  }
}
