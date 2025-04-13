package org.joshsim.lang.bridge;

import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.simulation.TimeStep;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.export.ExportFacade;
import org.joshsim.lang.export.ExportFacadeFactory;
import org.joshsim.lang.export.ExportTarget;
import org.joshsim.lang.export.ExportTargetParser;


/**
 * Convenience function that manages zero or more exports.
 */
public class CombinedExportFacade {

  private final Optional<ExportFacade> patchExportFacade;

  /**
   * Constructs a facade to manage export operations across multiple export files.
   *
   * @param simEntity the mutable entity representing the simulation context, used to retrieve and
   *     configure the patch export facade.
   */
  public CombinedExportFacade(MutableEntity simEntity) {
    patchExportFacade = getPatchExportFacade(simEntity);
  }

  /**
   * Write the result of a time step if requested.
   *
   * <p>Writes all entities from a given simulation time step to the export target, if the export
   * facade for that type is present. Each patch entity is processed and serialized along with the
   * time step number.</p>
   *
   * @param stepCompleted The completed time step containing the patches to be written
   *     and the step number to associate with them.
   */
  public void write(TimeStep stepCompleted) {
    patchExportFacade.ifPresent(exportFacade -> stepCompleted.getPatches().forEach(
        (x) -> exportFacade.write(x, stepCompleted.getStep())
    ));
  }

  /**
   * Starts the export process for the requested export facades.
   *
   * <p>Invokes the start method on the underlying ExportFacade instance associated
   * with the various export facades, if they exist. This triggers the dedicated writer
   * threads to begin processing and serializing entities for export.</p>
   */
  public void start() {
    patchExportFacade.ifPresent(ExportFacade::start);
  }

  /**
   * Waits for the completion of the export process, if a patch export facade is present.
   *
   * <p>Invokes the join method on the various underlying ExportFacade instances behind this facade,
   * ensuring all pending entities  in the export queue are processed and written before the
   * dedicated writer threads terminate.</p>
   */
  public void join() {
    patchExportFacade.ifPresent(ExportFacade::join);
  }

  /**
   * Retrieves the patch-specific export facade based on the provided simulation entity.
   *
   * @param simEntity the mutable entity representing the simulation context. It is used to fetch
   *     the attribute configuration and initialize the export facades.
   * @return an Optional containing the relevant ExportFacade or an empty Optional if configuration
   *     is not found.
   */
  private Optional<ExportFacade> getPatchExportFacade(MutableEntity simEntity) {
    return getExportFacade(simEntity, "exportFiles.patch");
  }

  /**
   * Retrieves an ExportFacade instance based on the given simulation entity and attribute.
   *
   * <p>This method starts a substep within the simulation entity, attempts to retrieve the
   * attribute value specified by the given attribute name, and constructs an ExportFacade if the
   * attribute value is valid. If no valid attribute value exists, an empty Optional is returned.
   * The substep is ended after the processing is completed.</p>
   *
   * @param simEntity the mutable entity representing the simulation context from which
   *     the attribute value is retrieved and the ExportFacade is constructed.
   * @param attribute the name of the attribute used to determine the target export configuration.
   * @return an Optional containing the constructed ExportFacade, or an empty Optional if
   *     the attribute value is not present or invalid.
   */
  private Optional<ExportFacade> getExportFacade(MutableEntity simEntity, String attribute) {
    simEntity.startSubstep("constant");
    Optional<EngineValue> destination = simEntity.getAttributeValue(attribute);

    Optional<ExportFacade> exportFacade;
    if (destination.isPresent()) {
      ExportTarget target = ExportTargetParser.parse(destination.get().getAsString());
      exportFacade = Optional.of(ExportFacadeFactory.build(target));
    } else {
      exportFacade = Optional.empty();
    }

    simEntity.endSubstep();
    return exportFacade;
  }

}
