/**
 * Structures to simplify writing debug messages to MinIO/S3.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import org.joshsim.lang.io.OutputStreamStrategy;

/**
 * DebugFacade that writes debug messages to MinIO/S3.
 *
 * <p>This is identical to FileDebugFacade since OutputStreamStrategy
 * abstracts the target (file vs MinIO). The OutputStreamStrategy implementation
 * handles the difference between local files and cloud storage.</p>
 */
public class MinioDebugFacade extends FileDebugFacade {

  /**
   * Constructs a MinioDebugFacade with the specified output strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing to MinIO/S3.
   */
  public MinioDebugFacade(OutputStreamStrategy outputStrategy) {
    super(outputStrategy);
  }
}
