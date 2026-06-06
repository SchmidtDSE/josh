/**
 * Tests for PreprocessBatchCommand helpers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for {@link PreprocessBatchCommand#resolveDataFileRelativeToInputDir}.
 *
 * <p>Covers the three paths the helper must handle: absolute paths under {@code inputDir}
 * (relativized), relative paths (passed through), and absolute paths outside {@code inputDir}
 * (rejected with a clear error).</p>
 */
public class PreprocessBatchCommandTest {

  @Test
  void absolutePathUnderInputDir_isRelativized(@TempDir Path tempDir) throws Exception {
    File inputDir = tempDir.toFile();
    String absoluteDataFile = tempDir.resolve("data.nc").toAbsolutePath().toString();

    String result = PreprocessBatchCommand.resolveDataFileRelativeToInputDir(
        absoluteDataFile, inputDir
    );

    assertEquals("data.nc", result);
  }

  @Test
  void absolutePathInSubdirectory_isRelativized(@TempDir Path tempDir) throws Exception {
    File inputDir = tempDir.toFile();
    Path subdir = tempDir.resolve("nested");
    subdir.toFile().mkdirs();
    String absoluteDataFile = subdir.resolve("data.nc").toAbsolutePath().toString();

    String result = PreprocessBatchCommand.resolveDataFileRelativeToInputDir(
        absoluteDataFile, inputDir
    );

    assertEquals(Path.of("nested", "data.nc").toString(), result);
  }

  @Test
  void relativePath_isPassedThrough() {
    File inputDir = new File("some/input/dir");

    String result = PreprocessBatchCommand.resolveDataFileRelativeToInputDir(
        "data.nc", inputDir
    );

    assertEquals("data.nc", result);
  }

  @Test
  void relativePathWithSubdirectory_isPassedThrough() {
    File inputDir = new File("some/input/dir");

    String result = PreprocessBatchCommand.resolveDataFileRelativeToInputDir(
        "nested/data.nc", inputDir
    );

    assertEquals("nested/data.nc", result);
  }

  @Test
  void absolutePathOutsideInputDir_throws(@TempDir Path tempDir) {
    File inputDir = tempDir.resolve("only-this-dir").toFile();
    inputDir.mkdirs();
    String absoluteDataFile = tempDir.resolve("elsewhere").resolve("data.nc")
        .toAbsolutePath().toString();

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> PreprocessBatchCommand.resolveDataFileRelativeToInputDir(absoluteDataFile, inputDir)
    );
    assertTrue(ex.getMessage().contains("outside input directory"),
        "Error should mention 'outside input directory', got: " + ex.getMessage());
  }
}
