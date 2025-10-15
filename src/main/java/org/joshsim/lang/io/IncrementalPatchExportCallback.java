/**
 * Incremental patch export callback for memory-efficient serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.lang.bridge.InnerEntityGetter;
import org.joshsim.lang.bridge.PatchExportCallback;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;
import org.joshsim.wire.NamedMap;


/**
 * Incremental patch export callback that freezes, serializes, and queues patches
 * immediately after they complete their substeps.
 *
 * <p>This callback reduces peak memory usage by avoiding bulk freeze operations
 * and distributing write load evenly across the simulation.</p>
 */
public class IncrementalPatchExportCallback implements PatchExportCallback {

  private final Optional<ExportFacade> patchExportFacade;
  private final Optional<ExportFacade> entityExportFacade;
  private final int replicateNumber;

  /**
   * Create a new incremental export callback.
   *
   * @param patchExportFacade Optional facade for exporting patch data
   * @param entityExportFacade Optional facade for exporting inner entity data
   * @param replicateNumber The replicate number for this simulation
   */
  public IncrementalPatchExportCallback(
      Optional<ExportFacade> patchExportFacade,
      Optional<ExportFacade> entityExportFacade,
      int replicateNumber) {
    this.patchExportFacade = patchExportFacade;
    this.entityExportFacade = entityExportFacade;
    this.replicateNumber = replicateNumber;
  }

  @Override
  public Entity exportPatch(MutableEntity patch, long currentStep) {
    // Step 1: Lock and freeze the patch
    patch.lock();
    Entity frozen;
    try {
      frozen = patch.freeze();
    } finally {
      patch.unlock();
    }

    // Step 2: Export patch data if requested
    patchExportFacade.ifPresent(facade -> {
      Optional<MapExportSerializeStrategy> strategy = facade.getSerializeStrategy();
      if (strategy.isPresent()) {
        // Producer serialization: serialize to Map and queue NamedMap
        Map<String, String> serialized = strategy.get().getRecord(frozen);
        NamedMap namedMap = new NamedMap(frozen.getName(), serialized);
        facade.write(namedMap, currentStep, replicateNumber);
      } else {
        // Legacy: queue Entity (serialization in writer thread)
        facade.write(frozen, currentStep, replicateNumber);
      }
    });

    // Step 3: Export inner entities if requested
    entityExportFacade.ifPresent(facade -> {
      Optional<MapExportSerializeStrategy> strategy = facade.getSerializeStrategy();
      if (strategy.isPresent()) {
        // Producer serialization for inner entities
        InnerEntityGetter.getInnerFrozenEntitiesRecursive(frozen).forEach(entity -> {
          Map<String, String> serialized = strategy.get().getRecord(entity);
          NamedMap namedMap = new NamedMap(entity.getName(), serialized);
          facade.write(namedMap, currentStep, replicateNumber);
        });
      } else {
        // Legacy: queue entities
        InnerEntityGetter.getInnerFrozenEntitiesRecursive(frozen)
            .forEach(entity -> facade.write(entity, currentStep, replicateNumber));
      }
    });

    // Step 4: Return frozen entity for Replicate.pastTimeSteps
    return frozen;
  }
}
