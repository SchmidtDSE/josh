/**
 * Structures to simplify writing entities to an in-memory callback.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.util.Map;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.entity.base.Entity;
import org.teavm.jso.JSBody;


/**
 * Strategy implementing ExportFacade which writes entities to an in-memory JS callback.
 */
public class JsExportFacade implements ExportFacade {

  private final String path;
  private final ExportSerializeStrategy<Map<String, String>> serializeStrategy;

  /**
   * Constructs a CsvExportFacade object with the specified export target / output stream strategy.
   */
  public JsExportFacade(String path) {
    this.path = path;
    serializeStrategy = new MapSerializeStrategy();
  }

  @Override
  public void start() {}

  @Override
  public void join() {}

  @Override
  public void write(Entity entity, long step) {
    Map<String, String> record = serializeStrategy.getRecord(entity);
    record.put("step", ((Long) step).toString());
    record.put("path", path);

    CompatibleStringJoiner joiner = CompatibilityLayerKeeper.get().createStringJoiner(";");

    for (String name : record.keySet()) {
      String value = record.get(name);
      joiner.add(String.format("%s = \"%s\"", name, value.replaceAll("\"", "\\\"")));
    }

    String joinedStr = joiner.toString();
  }

  @JSBody(params = { "reportRecord" }, script = "reportRecord(message)")
  private static native void reportRecord(String message);

}
