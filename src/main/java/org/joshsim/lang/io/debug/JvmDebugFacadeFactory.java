/**
 * JVM-specific implementation of DebugFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.LocalOutputStreamStrategy;
import org.joshsim.lang.io.MinioOutputStreamStrategy;
import org.joshsim.lang.io.OutputStreamStrategy;
import org.joshsim.util.MinioClientSingleton;
import org.joshsim.util.MinioOptions;

/**
 * JVM implementation of DebugFacadeFactory.
 *
 * <p>Creates appropriate DebugFacade instances based on the target protocol:
 * <ul>
 *   <li>stdout - writes to standard output</li>
 *   <li>memory - writes to memory (for web editor)</li>
 *   <li>minio - writes to MinIO/S3 storage</li>
 *   <li>file (or empty protocol) - writes to local file</li>
 * </ul>
 */
public class JvmDebugFacadeFactory implements DebugFacadeFactory {

  private final int replicate;
  private final MinioOptions minioOptions;

  /**
   * Create a new JvmDebugFacadeFactory.
   *
   * @param replicate The replicate number to use in filenames.
   * @param minioOptions The MinIO configuration options (nullable).
   */
  public JvmDebugFacadeFactory(int replicate, MinioOptions minioOptions) {
    this.replicate = replicate;
    this.minioOptions = minioOptions;
  }

  /**
   * Create a new JvmDebugFacadeFactory without MinIO support.
   *
   * @param replicate The replicate number to use in filenames.
   */
  public JvmDebugFacadeFactory(int replicate) {
    this(replicate, null);
  }

  @Override
  public DebugFacade build(ExportTarget target) {
    String protocol = target.getProtocol();

    if (protocol.equals("stdout")) {
      return new StdoutDebugFacade();
    } else if (protocol.equals("memory")) {
      OutputStreamStrategy strategy = createOutputStreamStrategy(target);
      return new MemoryDebugFacade(strategy);
    } else if (protocol.equals("minio")) {
      OutputStreamStrategy strategy = createOutputStreamStrategy(target);
      return new MinioDebugFacade(strategy);
    } else {
      // Default to file (including empty protocol)
      OutputStreamStrategy strategy = createOutputStreamStrategy(target);
      return new FileDebugFacade(strategy);
    }
  }

  @Override
  public String getPath(String template) {
    // Simple template - just replace replicate
    return template.replace("{replicate}", String.valueOf(replicate));
  }

  @Override
  public int getReplicateNumber() {
    return replicate;
  }

  /**
   * Creates the appropriate OutputStreamStrategy based on the target protocol.
   *
   * @param target The debug target containing protocol and path information.
   * @return OutputStreamStrategy for the specified protocol.
   * @throws IllegalArgumentException if protocol is unsupported or MinIO is not configured.
   */
  private OutputStreamStrategy createOutputStreamStrategy(ExportTarget target) {
    String protocol = target.getProtocol();

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
            "MinIO bucket name must be specified either in the URL (minio://bucket-name/path) "
            + "or via --minio-bucket option or MINIO_BUCKET environment variable"
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

    } else if (protocol.equals("memory")) {
      // Memory output - handled by caller, but provide default implementation
      throw new IllegalArgumentException(
        "Memory protocol requires special OutputStreamStrategy implementation"
      );

    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }
  }
}
