/**
 * Command line interface handler for inspecting export paths in Josh simulation files.
 *
 * <p>This class implements the 'inspect-exports' command which parses a Josh script file
 * and extracts the exportFiles paths (patch, meta, entity) from a specified simulation.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.ExportTargetParser;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for inspecting export paths in Josh simulation files.
 *
 * <p>Parses a Josh script file and extracts the exportFiles configuration from a specified
 * simulation. Outputs the export paths for patch, meta, and entity exports in JSON format
 * for programmatic consumption.</p>
 */
@Command(
    name = "inspect-exports",
    description = "Extract exportFiles paths from a simulation"
)
public class InspectExportsCommand implements Callable<Integer> {

  @Parameters(index = "0", description = "Path to Josh file to inspect")
  private File file;

  @Parameters(index = "1", description = "Name of the simulation to inspect")
  private String simulationName;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Option(
      names = "--json",
      description = "Output in JSON format (default: true)",
      defaultValue = "true"
  )
  private boolean jsonOutput = true;

  @Override
  public Integer call() {
    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        file,
        output
    );

    if (initResult.getFailureStep().isPresent()) {
      JoshSimCommander.CommanderStepEnum failStep = initResult.getFailureStep().get();
      return switch (failStep) {
        case LOAD -> 1;
        case READ -> 2;
        case PARSE -> 3;
        default -> 404;
      };
    }

    JoshProgram program = initResult.getProgram().orElseThrow();

    if (!program.getSimulations().hasPrototype(simulationName)) {
      output.printError("Could not find simulation: " + simulationName);
      return 4;
    }

    try {
      ValueSupportFactory valueFactory = new ValueSupportFactory();
      MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
      MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);

      simEntity.startSubstep("constant");

      Optional<EngineValue> patchExport = simEntity.getAttributeValue("exportFiles.patch");
      Optional<EngineValue> metaExport = simEntity.getAttributeValue("exportFiles.meta");
      Optional<EngineValue> entityExport = simEntity.getAttributeValue("exportFiles.entity");

      Optional<EngineValue> debugOrganism = simEntity.getAttributeValue("debugFiles.organism");
      Optional<EngineValue> debugPatch = simEntity.getAttributeValue("debugFiles.patch");
      Optional<EngineValue> debugAgent = simEntity.getAttributeValue("debugFiles.agent");
      Optional<EngineValue> debugDisturbance =
          simEntity.getAttributeValue("debugFiles.disturbance");

      simEntityRaw.endSubstep();

      if (jsonOutput) {
        outputJson(patchExport, metaExport, entityExport,
            debugOrganism, debugPatch, debugAgent, debugDisturbance);
      } else {
        outputPlain(patchExport, metaExport, entityExport,
            debugOrganism, debugPatch, debugAgent, debugDisturbance);
      }

      return 0;
    } catch (Exception e) {
      output.printError("Error extracting export paths: " + e.getMessage());
      return 5;
    }
  }

  private void outputJson(
      Optional<EngineValue> patchExport,
      Optional<EngineValue> metaExport,
      Optional<EngineValue> entityExport,
      Optional<EngineValue> debugOrganism,
      Optional<EngineValue> debugPatch,
      Optional<EngineValue> debugAgent,
      Optional<EngineValue> debugDisturbance
  ) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"simulation\": \"").append(escapeJson(simulationName)).append("\",\n");
    json.append("  \"exportFiles\": {\n");

    json.append("    \"patch\": ");
    appendExportJson(json, patchExport);
    json.append(",\n");

    json.append("    \"meta\": ");
    appendExportJson(json, metaExport);
    json.append(",\n");

    json.append("    \"entity\": ");
    appendExportJson(json, entityExport);
    json.append("\n");

    json.append("  },\n");
    json.append("  \"debugFiles\": {\n");

    json.append("    \"organism\": ");
    appendExportJson(json, debugOrganism);
    json.append(",\n");

    json.append("    \"patch\": ");
    appendExportJson(json, debugPatch);
    json.append(",\n");

    json.append("    \"agent\": ");
    appendExportJson(json, debugAgent);
    json.append(",\n");

    json.append("    \"disturbance\": ");
    appendExportJson(json, debugDisturbance);
    json.append("\n");

    json.append("  }\n");
    json.append("}");

    output.printInfo(json.toString());
  }

  private void appendExportJson(StringBuilder json, Optional<EngineValue> exportValue) {
    if (exportValue.isEmpty()) {
      json.append("null");
      return;
    }

    String rawPath = exportValue.get().getAsString();
    ExportTarget target = ExportTargetParser.parse(rawPath);

    json.append("{\n");
    json.append("      \"raw\": \"").append(escapeJson(rawPath)).append("\",\n");
    json.append("      \"protocol\": \"").append(escapeJson(target.getProtocol())).append("\",\n");
    json.append("      \"host\": \"").append(escapeJson(target.getHost())).append("\",\n");
    json.append("      \"path\": \"").append(escapeJson(target.getPath())).append("\",\n");
    json.append("      \"fileType\": \"").append(escapeJson(target.getFileType())).append("\"\n");
    json.append("    }");
  }

  private void outputPlain(
      Optional<EngineValue> patchExport,
      Optional<EngineValue> metaExport,
      Optional<EngineValue> entityExport,
      Optional<EngineValue> debugOrganism,
      Optional<EngineValue> debugPatch,
      Optional<EngineValue> debugAgent,
      Optional<EngineValue> debugDisturbance
  ) {
    output.printInfo("Simulation: " + simulationName);
    output.printInfo("Export Files:");
    output.printInfo("  patch: " + (patchExport.map(v -> v.getAsString()).orElse("(not defined)")));
    output.printInfo("  meta: " + (metaExport.map(v -> v.getAsString()).orElse("(not defined)")));
    output.printInfo(
          "  entity: " + (entityExport.map(v -> v.getAsString()).orElse("(not defined)"))
    );
    output.printInfo("Debug Files:");
    output.printInfo(
        "  organism: " + (debugOrganism.map(v -> v.getAsString()).orElse("(not defined)"))
    );
    output.printInfo("  patch: " + (debugPatch.map(v -> v.getAsString()).orElse("(not defined)")));
    output.printInfo("  agent: " + (debugAgent.map(v -> v.getAsString()).orElse("(not defined)")));
    output.printInfo(
        "  disturbance: " + (debugDisturbance.map(v -> v.getAsString()).orElse("(not defined)"))
    );
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
