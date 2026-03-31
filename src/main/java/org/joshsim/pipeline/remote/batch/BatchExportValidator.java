/**
 * Validates export paths for batch execution safety.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.ExportTargetParser;
import org.joshsim.util.OutputOptions;


/**
 * Validates that export paths in a Josh simulation are safe for concurrent batch execution.
 *
 * <p>Batch workers write to MinIO concurrently. Export paths must use the minio:// protocol
 * and contain {replicate} to ensure each worker writes to a unique location. This validator
 * is used both client-side (in BatchJobStrategy, before uploading inputs) and worker-side
 * (in RunFromMinioCommand, as a safety net).</p>
 */
public class BatchExportValidator {

  private static final String[] EXPORT_KEYS = {
      "exportFiles.patch", "exportFiles.meta", "exportFiles.entity"
  };

  private static final String[] DEBUG_KEYS = {
      "debugFiles.organism", "debugFiles.patch", "debugFiles.agent",
      "debugFiles.disturbance"
  };

  /**
   * Validates that all export paths in a simulation are safe for batch execution.
   *
   * @param joshFile the Josh simulation file
   * @param simulation the simulation name to check
   * @param output for reporting errors
   * @return list of validation error messages (empty if valid)
   */
  public static List<String> validate(File joshFile, String simulation, OutputOptions output) {
    List<String> errors = new ArrayList<>();

    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        joshFile,
        output
    );

    if (initResult.getFailureStep().isPresent()) {
      errors.add("Failed to parse Josh file for export validation");
      return errors;
    }

    JoshProgram program = initResult.getProgram().orElseThrow();

    if (!program.getSimulations().hasPrototype(simulation)) {
      errors.add("Could not find simulation: " + simulation);
      return errors;
    }

    try {
      EngineValueFactory valueFactory = new EngineValueFactory();
      MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
      MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
      simEntity.startSubstep("constant");

      validateTargets(simEntity, EXPORT_KEYS, "exportFiles", errors);
      validateTargets(simEntity, DEBUG_KEYS, "debugFiles", errors);

      simEntityRaw.endSubstep();
    } catch (Exception e) {
      errors.add("Error validating export paths: " + e.getMessage());
    }

    return errors;
  }

  /**
   * Validates a set of export/debug targets for batch safety.
   *
   * @param simEntity the simulation entity to extract paths from
   * @param keys the attribute keys to check
   * @param category the category name for error messages
   * @param errors list to append validation errors to
   */
  private static void validateTargets(
      MutableEntity simEntity,
      String[] keys,
      String category,
      List<String> errors
  ) {
    for (String key : keys) {
      Optional<EngineValue> value = simEntity.getAttributeValue(key);
      if (value.isEmpty()) {
        continue;
      }

      String rawPath = value.get().getAsString();
      ExportTarget target = ExportTargetParser.parse(rawPath);
      String uri = target.toUri();

      if (!rawPath.contains("{replicate}")) {
        errors.add(
            "Batch execution requires {replicate} in all export paths. "
            + category + " path '" + uri + "' (from " + key + ") "
            + "does not contain {replicate}. This is required because batch workers "
            + "write to MinIO concurrently and cannot safely share output files. "
            + "Use a path like: minio://bucket/results/output_{replicate}.csv"
        );
      }

      if (!"minio".equals(target.getProtocol())) {
        errors.add(
            "Batch execution requires minio:// protocol for all export paths. "
            + category + " path '" + uri + "' (from " + key + ") "
            + "uses a non-MinIO protocol. Results written to local paths inside "
            + "containers are lost on termination."
        );
      }
    }
  }
}
