/**
 * WebAssembly-compatible OutputWriterFactory that only creates memory and stdout writers.
 *
 * <p>This factory is used in sandbox environments (WebAssembly and remote workers) and only
 * creates strategies that are compatible with TeaVM compilation. It never references JVM-only
 * classes like LocalOutputStreamStrategy or MinioOutputStreamStrategy.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;

/**
 * OutputWriterFactory implementation for sandbox environments.
 *
 * <p>This implementation only creates WebAssembly-compatible output strategies,
 * avoiding JVM-specific classes that would cause TeaVM compilation errors.</p>
 */
public class SandboxOutputWriterFactory extends OutputWriterFactory {

  private final SandboxExportCallback callback;
  private final boolean allowAllProtocols;

  /**
   * Creates a new SandboxOutputWriterFactory.
   *
   * @param replicate The replicate number (usually 0 for sandbox mode)
   * @param templateResolver The template resolver for {replicate} and custom tags
   * @param callback The sandbox callback for memory:// output
   * @param allowAllProtocols If true, accept any protocol and redirect through callback
   */
  public SandboxOutputWriterFactory(int replicate,
                                    PathTemplateResolver templateResolver,
                                    SandboxExportCallback callback,
                                    boolean allowAllProtocols) {
    super(replicate, templateResolver, null, null, callback, allowAllProtocols);
    this.callback = callback;
    this.allowAllProtocols = allowAllProtocols;
  }

  @Override
  public OutputWriter<String> createTextWriter(String targetUri) {
    // Resolve template variables
    String resolvedUri = resolvePath(targetUri);

    // Parse the URI
    OutputTarget target = parseTarget(resolvedUri);

    // Create WebAssembly-compatible strategy
    OutputStreamStrategy strategy = createSandboxStrategy(target);

    // Create and return the TextOutputWriter
    return new TextOutputWriter(target, strategy);
  }

  @Override
  public OutputWriter<Map<String, String>> createStructuredWriter(String targetUri) {
    // Sandbox mode doesn't use this for debug output, but implement for completeness
    String resolvedUri = resolvePath(targetUri);
    OutputTarget target = parseTarget(resolvedUri);
    OutputStreamStrategy strategy = createSandboxStrategy(target);
    return new StructuredOutputWriter(target, strategy, 0);
  }

  /**
   * Creates WebAssembly-compatible output stream strategy.
   *
   * <p>This method only references WebAssembly-compatible classes, avoiding
   * TeaVM compilation issues with JVM-only classes.</p>
   *
   * @param target The output target
   * @return WebAssembly-compatible OutputStreamStrategy
   */
  private OutputStreamStrategy createSandboxStrategy(OutputTarget target) {
    String protocol = target.getProtocol().toLowerCase();

    if (allowAllProtocols) {
      // Remote workers: accept ANY protocol but redirect through callback
      return new MemoryOutputStreamStrategy(callback);
    }

    // Pure WebAssembly: only memory:// and stdout:// allowed
    if (protocol.equals("memory")) {
      return new MemoryOutputStreamStrategy(callback);
    } else if (protocol.equals("stdout")) {
      return new StdoutOutputStreamStrategy();
    } else {
      throw new IllegalArgumentException(
        "Only memory:// and stdout:// protocols are supported in WebAssembly mode. "
        + "Found: " + protocol + "://"
      );
    }
  }
}
