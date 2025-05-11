/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;
import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Interface for a strategy providing JVM-specific input / output operations.
 */
public class JvmInputOutputLayer implements InputOutputLayer {

  private final JvmExportFacadeFactory exportFactory;

  /**
   * Create a new input / output layer with grid-space only.
   */
  public JvmInputOutputLayer() {
    exportFactory = new JvmExportFacadeFactory();
  }

  /**
   * Create a new input / output layer with access to Earth-space.
   */
  public JvmInputOutputLayer(PatchBuilderExtents extents, BigDecimal width) {
    exportFactory = new JvmExportFacadeFactory(extents, width);
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return exportFactory;
  }

  @Override
  public InputGetterStrategy getInputStrategy() {
    return new JvmInputGetter();
  }

}
