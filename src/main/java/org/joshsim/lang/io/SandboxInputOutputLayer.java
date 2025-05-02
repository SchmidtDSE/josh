/**
 * Structures to provide access to input / output operations when running in a sandbox.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


/**
 * Interface for a strategy providing input / output operations when in a sandbox.
 *
 * <p>Interface for a strategy providing input / output operations when in a sandbox, including
 * WebAssembly-specific input / output operations.</p>
 */
public class SandboxInputOutputLayer implements InputOutputLayer {

  private final SandboxExportCallback callback;

  /**
   * Create a new I/O layer using the given callback for when export data are generated.
   */
  public SandboxInputOutputLayer(SandboxExportCallback callback) {
    this.callback = callback;
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new SandboxExportFacadeFactory(callback);
  }

}
