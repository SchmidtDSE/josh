/**
 * Sandbox implementation of DebugFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.PathTemplateResolver;

/**
 * Sandbox implementation of DebugFacadeFactory for WebAssembly and sandboxed environments.
 *
 * <p>In sandbox mode, debug output can go to memory or stdout only, not to files or MinIO.</p>
 */
public class SandboxDebugFacadeFactory implements DebugFacadeFactory {

  private final PathTemplateResolver templateResolver;

  /**
   * Create a new SandboxDebugFacadeFactory with template resolver.
   *
   * @param templateResolver The template resolver for processing path templates (nullable).
   */
  public SandboxDebugFacadeFactory(PathTemplateResolver templateResolver) {
    this.templateResolver = templateResolver != null
        ? templateResolver
        : new PathTemplateResolver();
  }

  /**
   * Create a new SandboxDebugFacadeFactory with no template resolver (legacy constructor).
   *
   * @deprecated Use constructor with PathTemplateResolver parameter instead
   */
  @Deprecated
  public SandboxDebugFacadeFactory() {
    this(null);
  }

  @Override
  public DebugFacade build(ExportTarget target) {
    String protocol = target.getProtocol();

    if (protocol.equals("stdout") || protocol.isEmpty() || protocol.equals("file")) {
      // In sandbox, default to stdout
      return new StdoutDebugFacade();
    } else if (protocol.equals("memory")) {
      // Memory debug not yet implemented for sandbox
      throw new IllegalArgumentException(
        "Memory debug output not yet implemented for sandbox environment"
      );
    } else {
      throw new IllegalArgumentException(
        "Unsupported protocol in sandbox: " + protocol + " (use stdout or memory)"
      );
    }
  }

  @Override
  public String getPath(String template) {
    // Use template resolver even in sandbox for consistency
    return templateResolver.resolve(template, 0);
  }

  @Override
  public int getReplicateNumber() {
    return 0; // Sandbox typically runs single replicate
  }
}
