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
    Entity frozen = freezePatch(patch);
    exportPatchData(frozen, currentStep);
    exportInnerEntities(frozen, currentStep);
    return frozen;
  }

  /**
   * Locks and freezes the patch entity.
   *
   * @param patch The mutable patch entity to freeze
   * @return The frozen entity
   */
  private Entity freezePatch(MutableEntity patch) {
    patch.lock();
    try {
      return patch.freeze();
    } finally {
      patch.unlock();
    }
  }

  /**
   * Exports patch data if a patch export facade is configured.
   *
   * @param frozen The frozen patch entity to export
   * @param currentStep The current simulation step
   */
  private void exportPatchData(Entity frozen, long currentStep) {
    patchExportFacade.ifPresent(facade -> {
      Optional<MapExportSerializeStrategy> strategy = facade.getSerializeStrategy();
      boolean producerSerializing = strategy.isPresent();

      if (producerSerializing) {
        Map<String, String> serialized = strategy.get().getRecord(frozen);
        NamedMap namedMap = new NamedMap(frozen.getName(), serialized);
        facade.write(namedMap, currentStep, replicateNumber);
      } else {
        facade.write(frozen, currentStep, replicateNumber);
      }
    });
  }

  /**
   * Exports inner entities if an entity export facade is configured.
   *
   * @param frozen The frozen patch entity containing inner entities
   * @param currentStep The current simulation step
   */
  private void exportInnerEntities(Entity frozen, long currentStep) {
    entityExportFacade.ifPresent(facade -> {
      Optional<MapExportSerializeStrategy> strategy = facade.getSerializeStrategy();
      boolean producerSerializing = strategy.isPresent();

      if (producerSerializing) {
        InnerEntityGetter.getInnerFrozenEntitiesRecursive(frozen).forEach(entity -> {
          Map<String, String> serialized = strategy.get().getRecord(entity);
          NamedMap namedMap = new NamedMap(entity.getName(), serialized);
          facade.write(namedMap, currentStep, replicateNumber);
        });
      } else {
        InnerEntityGetter.getInnerFrozenEntitiesRecursive(frozen)
            .forEach(entity -> facade.write(entity, currentStep, replicateNumber));
      }
    });
  }
}
