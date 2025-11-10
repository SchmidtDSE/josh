/**
 * Factory for creating OutputWriter instances for different output types and destinations.
 *
 * <p>This factory provides a unified interface for creating output writers for both text-based
 * debug output and structured export output. It handles URI parsing, template resolution,
 * MinIO configuration, and writer creation based on the target destination protocol.</p>
 *
 * <p>The factory supports creating:</p>
 * <ul>
 *   <li>Text writers for debug output (writing String data)</li>
 *   <li>Structured writers for CSV export (writing DataRow data)</li>
 *   <li>Combined writers that route to per-entity-type writers</li>
 * </ul>
 *
 * <p>Supported destination protocols:</p>
 * <ul>
 *   <li><b>file://</b> - Local file system</li>
 *   <li><b>minio://</b> - MinIO or S3-compatible storage</li>
 *   <li><b>stdout://</b> - Standard output stream</li>
 *   <li><b>memory://</b> - In-memory storage (for web editor)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * OutputWriterFactory factory = new OutputWriterFactory(
 *     replicate,
 *     templateResolver,
 *     minioOptions
 * );
 *
 * // Create a text writer for debug output
 * OutputWriter&lt;String&gt; debugWriter =
 *     factory.createTextWriter("file:///tmp/debug_{replicate}.txt");
 *
 * // Create a structured writer for CSV export
 * OutputWriter&lt;DataRow&gt; exportWriter =
 *     factory.createStructuredWriter("minio://bucket/export.csv");
 * </pre>
 *
 * <p>This factory replaces the separate DebugFacadeFactory and ExportFacadeFactory with a
 * unified implementation that serves both use cases.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioClientSingleton;
import org.joshsim.util.MinioOptions;

/**
 * Factory for creating OutputWriter instances.
 *
 * <p>This factory handles the complexity of determining the appropriate OutputWriter
 * implementation based on the target URI protocol, configuring MinIO clients for remote
 * storage, and resolving path templates with replicate numbers and custom tags.</p>
 */
public class OutputWriterFactory {

  private final int replicate;
  private final PathTemplateResolver templateResolver;
  private final TemplateStringRenderer jobTemplateRenderer;
  private final MinioOptions minioOptions;

  /**
   * Creates a new OutputWriterFactory.
   *
   * <p>This is the primary constructor that should be used for creating the factory with
   * full configuration including MinIO support and path template resolution.</p>
   *
   * @param replicate The replicate number to use in path template resolution
   * @param templateResolver The template resolver for processing {replicate}, {user},
   *                        and other custom tags in paths (nullable, defaults to basic resolver)
   * @param jobTemplateRenderer The job template renderer for processing job-specific templates
   *                           like {editor} from data file mappings (nullable)
   * @param minioOptions The MinIO configuration options for remote storage (nullable, MinIO
   *                    destinations will not be available if null)
   */
  public OutputWriterFactory(int replicate, PathTemplateResolver templateResolver,
                            TemplateStringRenderer jobTemplateRenderer,
                            MinioOptions minioOptions) {
    this.replicate = replicate;
    this.templateResolver = templateResolver != null
        ? templateResolver
        : new PathTemplateResolver();
    this.jobTemplateRenderer = jobTemplateRenderer;
    this.minioOptions = minioOptions;
  }

  /**
   * Creates a new OutputWriterFactory without MinIO support.
   *
   * <p>This constructor is provided for convenience when MinIO support is not needed.
   * Attempts to create writers for minio:// destinations will fail.</p>
   *
   * @param replicate The replicate number to use in path template resolution
   * @param templateResolver The template resolver for processing path templates (nullable)
   */
  public OutputWriterFactory(int replicate, PathTemplateResolver templateResolver) {
    this(replicate, templateResolver, null, null);
  }

  /**
   * Creates a new OutputWriterFactory with minimal configuration.
   *
   * <p>This constructor is provided for simple use cases that don't need MinIO support
   * or custom path templates. Only {replicate} template variable will be available.</p>
   *
   * @param replicate The replicate number to use in path template resolution
   */
  public OutputWriterFactory(int replicate) {
    this(replicate, null, null, null);
  }

  /**
   * Creates a text writer for debug output.
   *
   * <p>Creates an OutputWriter&lt;String&gt; that writes plain text debug messages to the
   * specified target URI. The target URI is parsed to determine the destination type
   * (file, MinIO, stdout, etc.) and an appropriate writer implementation is created.</p>
   *
   * <p>Path template variables like {replicate}, {user}, {editor} are resolved before
   * the writer is created.</p>
   *
   * @param targetUri The destination URI
   *                  (e.g., "file:///tmp/debug.txt", "minio://bucket/debug.txt")
   * @return An OutputWriter that writes String data to the specified destination
   * @throws IllegalArgumentException if the target URI is invalid or unsupported
   */
  public OutputWriter<String> createTextWriter(String targetUri) {
    // Resolve template variables in the URI
    String resolvedUri = resolvePath(targetUri);

    // Parse the URI to get target components
    OutputTarget target = parseTarget(resolvedUri);

    // Create the appropriate OutputStreamStrategy based on the protocol
    OutputStreamStrategy strategy = createOutputStreamStrategy(target);

    // Create and return the TextOutputWriter
    return new TextOutputWriter(target, strategy);
  }

  /**
   * Creates a structured writer for export output.
   *
   * <p>Creates an OutputWriter that writes structured data (like CSV rows) to the specified
   * target URI. The target URI is parsed to determine the destination type and an appropriate
   * writer implementation is created.</p>
   *
   * <p>Path template variables are resolved before the writer is created.</p>
   *
   * @param targetUri The destination URI
   *                  (e.g., "file:///tmp/export.csv", "minio://bucket/export.csv")
   * @return An OutputWriter that writes structured data to the specified destination
   * @throws IllegalArgumentException if the target URI is invalid or unsupported
   */
  public OutputWriter<Map<String, String>> createStructuredWriter(String targetUri) {
    // Resolve template variables in the URI
    String resolvedUri = resolvePath(targetUri);

    // Parse the URI to get target components
    OutputTarget target = parseTarget(resolvedUri);

    // Create the appropriate OutputStreamStrategy based on the protocol
    // For CSV export, use append mode for consolidated multi-replicate files
    OutputStreamStrategy strategy = createStructuredOutputStreamStrategy(target);

    // Create and return the StructuredOutputWriter
    return new StructuredOutputWriter(target, strategy, replicate);
  }

  /**
   * Creates a structured writer with specified column headers.
   *
   * <p>Creates an OutputWriter that writes structured data with a predefined set of column
   * headers. This is useful when you want to enforce a specific column order or subset.</p>
   *
   * @param targetUri The destination URI
   * @param header Iterable of column names to use for the CSV header
   * @return An OutputWriter that writes structured data with specified headers
   * @throws IllegalArgumentException if the target URI is invalid or unsupported
   */
  public OutputWriter<Map<String, String>> createStructuredWriter(String targetUri,
                                                                   Iterable<String> header) {
    // Resolve template variables in the URI
    String resolvedUri = resolvePath(targetUri);

    // Parse the URI to get target components
    OutputTarget target = parseTarget(resolvedUri);

    // Create the appropriate OutputStreamStrategy based on the protocol
    OutputStreamStrategy strategy = createStructuredOutputStreamStrategy(target);

    // Create and return the StructuredOutputWriter with headers
    return new StructuredOutputWriter(target, strategy, replicate, header);
  }

  /**
   * Creates a combined text writer that routes to per-entity-type writers.
   *
   * <p>Creates a CombinedTextWriter that routes debug messages to different output files
   * based on the entity type. This is useful when you want separate debug files for
   * different entity types (e.g., one file for "ForeverTree" entities, another for "patch").</p>
   *
   * <p>The map should contain entity type names as keys and target URIs as values.
   * Each URI will have template variables resolved before creating the writer.</p>
   *
   * @param targetsByEntityType Map from entity type names to their target URIs
   * @return A combined OutputWriter that routes by entity type
   * @throws IllegalArgumentException if any target URI is invalid or unsupported
   */
  public OutputWriter<String> createCombinedTextWriter(
      Map<String, String> targetsByEntityType) {
    Map<String, OutputWriter<String>> writersByEntityType = new HashMap<>();

    // Create a text writer for each entity type
    for (Map.Entry<String, String> entry : targetsByEntityType.entrySet()) {
      String entityType = entry.getKey();
      String targetUri = entry.getValue();
      OutputWriter<String> writer = createTextWriter(targetUri);
      writersByEntityType.put(entityType, writer);
    }

    return new CombinedTextWriter(writersByEntityType);
  }

  /**
   * Creates a combined structured writer that routes to per-entity-type writers.
   *
   * <p>Creates a CombinedStructuredWriter that routes export data to different output files
   * based on the entity type. This enables separate export files for different entity types.</p>
   *
   * <p>The map should contain entity type names as keys and target URIs as values.
   * Each URI will have template variables resolved before creating the writer.</p>
   *
   * @param targetsByEntityType Map from entity type names to their target URIs
   * @return A combined OutputWriter that routes by entity type
   * @throws IllegalArgumentException if any target URI is invalid or unsupported
   */
  public OutputWriter<Map<String, String>> createCombinedStructuredWriter(
      Map<String, String> targetsByEntityType) {
    Map<String, OutputWriter<Map<String, String>>> writersByEntityType = new HashMap<>();

    // Create a structured writer for each entity type
    for (Map.Entry<String, String> entry : targetsByEntityType.entrySet()) {
      String entityType = entry.getKey();
      String targetUri = entry.getValue();
      OutputWriter<Map<String, String>> writer = createStructuredWriter(targetUri);
      writersByEntityType.put(entityType, writer);
    }

    return new CombinedStructuredWriter(writersByEntityType);
  }

  /**
   * Resolves path template variables in the given template string.
   *
   * <p>This method performs template resolution in two phases:</p>
   * <ol>
   *   <li>First, resolves job-specific templates like {editor} using TemplateStringRenderer
   *       (if available)</li>
   *   <li>Then, resolves {replicate} and custom tags using PathTemplateResolver</li>
   * </ol>
   *
   * <p>This two-phase approach ensures that both job file mappings (e.g., editor.jshc)
   * and custom command-line tags (e.g., --custom-tag user=nick) are properly resolved.</p>
   *
   * @param template The template string containing variables in {name} format
   * @return The resolved path with all template variables replaced
   */
  public String resolvePath(String template) {
    // Phase 1: Resolve job-specific templates like {editor} if renderer is available
    String afterJobTemplates = template;
    if (jobTemplateRenderer != null) {
      afterJobTemplates = jobTemplateRenderer.renderTemplate(template).getProcessedTemplate();
    }

    // Phase 2: Resolve {replicate} and custom tags
    return templateResolver.resolve(afterJobTemplates, replicate);
  }

  /**
   * Parses a target URI string into an OutputTarget object.
   *
   * <p>This is a utility method that delegates to ExportTargetParser to parse URIs.
   * It's provided here for convenience and will be used internally by the create methods.</p>
   *
   * @param targetUri The URI string to parse
   * @return An OutputTarget object representing the parsed URI
   * @throws IllegalArgumentException if the URI is invalid or unsupported
   */
  public OutputTarget parseTarget(String targetUri) {
    // Delegate to ExportTargetParser and convert ExportTarget to OutputTarget
    ExportTarget exportTarget = ExportTargetParser.parse(targetUri);

    // Convert ExportTarget to OutputTarget (they have the same structure)
    return new OutputTarget(
        exportTarget.getProtocol(),
        exportTarget.getHost(),
        exportTarget.getPath()
    );
  }

  /**
   * Gets the replicate number.
   *
   * @return The replicate number used by this factory
   */
  public int getReplicateNumber() {
    return replicate;
  }

  /**
   * Gets the path template resolver.
   *
   * @return The PathTemplateResolver used by this factory
   */
  public PathTemplateResolver getTemplateResolver() {
    return templateResolver;
  }

  /**
   * Gets the MinIO options if configured.
   *
   * @return The MinioOptions if available, null otherwise
   */
  public MinioOptions getMinioOptions() {
    return minioOptions;
  }

  /**
   * Checks if MinIO support is available.
   *
   * @return true if MinioOptions are configured, false otherwise
   */
  public boolean hasMinioSupport() {
    return minioOptions != null;
  }

  @Override
  public String toString() {
    return String.format("OutputWriterFactory{replicate=%d, hasMinioSupport=%s}",
        replicate, hasMinioSupport());
  }

  /**
   * Creates the appropriate OutputStreamStrategy based on the target protocol (for text output).
   *
   * <p>This is a private helper method that creates the correct OutputStreamStrategy
   * implementation based on the protocol specified in the OutputTarget. It handles
   * file, MinIO, stdout, and memory protocols.</p>
   *
   * @param target The output target containing protocol and path information
   * @return OutputStreamStrategy for the specified protocol
   * @throws IllegalArgumentException if protocol is unsupported or MinIO is not configured
   */
  private OutputStreamStrategy createOutputStreamStrategy(OutputTarget target) {
    String protocol = target.getProtocol().toLowerCase();

    if (protocol.isEmpty() || protocol.equals("file")) {
      // Local file system - use append mode for debug output
      return new LocalOutputStreamStrategy(target.getPath(), true);

    } else if (protocol.equals("minio")) {
      // MinIO direct streaming
      if (minioOptions == null || !minioOptions.isMinioOutput()) {
        throw new IllegalArgumentException(
          "MinIO protocol 'minio://" + target.getHost() + target.getPath() + "' "
          + "requires MinIO configuration (--minio-endpoint, --minio-access-key, etc.)"
        );
      }

      // Use bucket name from URL if specified, otherwise from MinioOptions
      String bucketName = target.getHost();
      if (bucketName == null || bucketName.isEmpty()) {
        bucketName = minioOptions.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
          throw new IllegalArgumentException(
            "MinIO bucket name must be specified either in the URL "
            + "(minio://bucket-name/path) or via --minio-bucket option "
            + "or MINIO_BUCKET environment variable"
          );
        }
      }

      // Strip leading slash from object path
      String objectPath = target.getPath();
      if (objectPath.startsWith("/")) {
        objectPath = objectPath.substring(1);
      }

      return new MinioOutputStreamStrategy(
        MinioClientSingleton.getInstance(minioOptions),
        bucketName,
        objectPath
      );

    } else if (protocol.equals("stdout")) {
      // Stdout - return a strategy that writes to System.out
      return new StdoutOutputStreamStrategy();

    } else if (protocol.equals("memory")) {
      // Memory output - throw exception for now, needs special handling
      throw new IllegalArgumentException(
        "Memory protocol requires special OutputStreamStrategy implementation. "
        + "This will be supported in a future phase."
      );

    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }
  }

  /**
   * Creates the appropriate OutputStreamStrategy for structured (CSV) output.
   *
   * <p>Similar to createOutputStreamStrategy but tailored for CSV export. Uses append mode
   * for local files to support multi-replicate consolidated output.</p>
   *
   * @param target The output target containing protocol and path information
   * @return OutputStreamStrategy for the specified protocol
   * @throws IllegalArgumentException if protocol is unsupported or MinIO is not configured
   */
  private OutputStreamStrategy createStructuredOutputStreamStrategy(OutputTarget target) {
    String protocol = target.getProtocol().toLowerCase();

    if (protocol.isEmpty() || protocol.equals("file")) {
      // Local file system - use append mode for consolidated multi-replicate files
      return new LocalOutputStreamStrategy(target.getPath(), true);

    } else if (protocol.equals("minio")) {
      // MinIO direct streaming
      if (minioOptions == null || !minioOptions.isMinioOutput()) {
        throw new IllegalArgumentException(
          "MinIO protocol 'minio://" + target.getHost() + target.getPath() + "' "
          + "requires MinIO configuration (--minio-endpoint, --minio-access-key, etc.)"
        );
      }

      // Use bucket name from URL if specified, otherwise from MinioOptions
      String bucketName = target.getHost();
      if (bucketName == null || bucketName.isEmpty()) {
        bucketName = minioOptions.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
          throw new IllegalArgumentException(
            "MinIO bucket name must be specified either in the URL "
            + "(minio://bucket-name/path) or via --minio-bucket option "
            + "or MINIO_BUCKET environment variable"
          );
        }
      }

      // Strip leading slash from object path
      String objectPath = target.getPath();
      if (objectPath.startsWith("/")) {
        objectPath = objectPath.substring(1);
      }

      return new MinioOutputStreamStrategy(
        MinioClientSingleton.getInstance(minioOptions),
        bucketName,
        objectPath
      );

    } else if (protocol.equals("stdout")) {
      // CSV to stdout is not typically useful, but allow it
      throw new IllegalArgumentException(
        "stdout:// protocol is not supported for CSV export. "
        + "Use file:// or minio:// instead."
      );

    } else if (protocol.equals("memory")) {
      // Memory output - throw exception for now, needs special handling
      throw new IllegalArgumentException(
        "Memory protocol requires special OutputStreamStrategy implementation. "
        + "This will be supported in a future phase."
      );

    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }
  }
}
