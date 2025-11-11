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
  private final boolean allowTemplates;

  /**
   * Create a new I/O layer using the given callback for when export data are generated.
   *
   * @param virtualFiles Mapping from file path to the virtual file which can be found at that
   *     location.
   * @param callback Callback to invoke when export data are available.
   */
  public SandboxInputOutputLayer(Map<String, VirtualFile> virtualFiles,
        SandboxExportCallback callback) {
    this(virtualFiles, callback, false); // Default: reject templates for WebAssembly security
  }

  /**
   * Create a new I/O layer using the given callback for when export data are generated.
   *
   * @param virtualFiles Mapping from file path to the virtual file which can be found at that
   *     location.
   * @param callback Callback to invoke when export data are available.
   * @param allowTemplates If true, template variables in export paths are allowed (for remote
   *     workers that stream results). If false, template variables are rejected (for WebAssembly).
   */
  public SandboxInputOutputLayer(Map<String, VirtualFile> virtualFiles,
        SandboxExportCallback callback, boolean allowTemplates) {
    this.inputGetter = new SandboxInputGetter(virtualFiles);
    this.callback = callback;
    this.allowTemplates = allowTemplates;
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new SandboxExportFacadeFactory(callback, allowTemplates);
  }

  @Override
  public OutputWriterFactory getOutputWriterFactory() {
    // In sandbox mode, use SandboxOutputWriterFactory which only creates WebAssembly-compatible
    // strategies. This avoids TeaVM trying to compile JVM-only LocalOutputStreamStrategy
    // - allowTemplates=true (remote workers): redirect ALL protocols through callback
    // - allowTemplates=false (pure WebAssembly): only memory:// and stdout:// allowed
    // Replicate 0 since sandbox typically runs single replicate
    return new SandboxOutputWriterFactory(0, new PathTemplateResolver(), callback, allowTemplates);
  }

  @Override
  public InputGetterStrategy getInputStrategy() {
    return inputGetter;
  }

}
