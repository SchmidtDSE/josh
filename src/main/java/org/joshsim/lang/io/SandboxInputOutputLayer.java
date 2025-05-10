/**
 * Structures to provide access to input / output operations when running in a sandbox.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;


/**
 * Interface for a strategy providing input / output operations when in a sandbox.
 *
 * <p>Interface for a strategy providing input / output operations when in a sandbox, including
 * WebAssembly-specific input / output operations.</p>
 */
public class SandboxInputOutputLayer implements InputOutputLayer {

  private final SandboxInputGetter inputGetter;
  private final SandboxExportCallback callback;

  /**
   * Create a new I/O layer using the given callback for when export data are generated.
   *
   * @param virtualFiles Mapping from file path to the virtual file which can be found at that
   *     location.
   * @param callback Callback to invoke when export data are available.
   */
  public SandboxInputOutputLayer(Map<String, VirtualFile> virtualFiles,
        SandboxExportCallback callback) {
    this.inputGetter = new SandboxInputGetter(virtualFiles);
    this.callback = callback;
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new SandboxExportFacadeFactory(callback);
  }

  @Override
  public InputGetterStrategy getInputStrategy() {
    return inputGetter;
  }

}
