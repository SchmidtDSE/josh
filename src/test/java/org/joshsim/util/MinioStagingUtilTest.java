/**
 * Tests for MinioStagingUtil.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for {@link MinioStagingUtil}, focused on the {@code .josh-staged.json}
 * sentinel skip and the "no data files" guard.
 */
public class MinioStagingUtilTest {

  private MinioHandler minio;
  private OutputOptions output;

  @TempDir
  File tempDir;

  @BeforeEach
  void setUp() {
    minio = mock(MinioHandler.class);
    output = mock(OutputOptions.class);
  }

  private void stubDownload(String relativePath, String contents) throws Exception {
    doAnswer(inv -> {
      File dest = inv.getArgument(1);
      Files.writeString(dest.toPath(), contents);
      return null;
    }).when(minio).downloadFile(eq(relativePath), any(File.class));
  }

  @Test
  void skipsSentinelAtPrefixRoot() throws Exception {
    when(minio.listObjects(anyString())).thenReturn(List.of(
        "batch-jobs/foo/inputs/a.txt",
        "batch-jobs/foo/inputs/.josh-staged.json"
    ));
    stubDownload("batch-jobs/foo/inputs/a.txt", "hello");

    int downloaded = MinioStagingUtil.stageFromMinio(
        minio, "batch-jobs/foo/inputs/", tempDir, output);

    assertEquals(1, downloaded);
    assertTrue(new File(tempDir, "a.txt").exists());
    assertFalse(new File(tempDir, ".josh-staged.json").exists());
    verify(minio, never()).downloadFile(eq("batch-jobs/foo/inputs/.josh-staged.json"),
        any(File.class));
  }

  @Test
  void skipsNestedSentinelFiles() throws Exception {
    when(minio.listObjects(anyString())).thenReturn(List.of(
        "batch-jobs/foo/inputs/.josh-staged.json",
        "batch-jobs/foo/inputs/sub/.josh-staged.json",
        "batch-jobs/foo/inputs/sub/data.csv"
    ));
    stubDownload("batch-jobs/foo/inputs/sub/data.csv", "x,y");

    int downloaded = MinioStagingUtil.stageFromMinio(
        minio, "batch-jobs/foo/inputs/", tempDir, output);

    assertEquals(1, downloaded);
    assertTrue(new File(new File(tempDir, "sub"), "data.csv").exists());
    assertFalse(new File(tempDir, ".josh-staged.json").exists());
    assertFalse(new File(new File(tempDir, "sub"), ".josh-staged.json").exists());
  }

  @Test
  void throwsWhenPrefixContainsOnlySentinel() throws Exception {
    when(minio.listObjects(anyString())).thenReturn(List.of(
        "batch-jobs/foo/inputs/.josh-staged.json"
    ));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> MinioStagingUtil.stageFromMinio(minio, "batch-jobs/foo/inputs/", tempDir, output));
    assertTrue(ex.getMessage().contains("No input objects"));
  }

  @Test
  void throwsWhenPrefixIsEmpty() throws Exception {
    when(minio.listObjects(anyString())).thenReturn(List.of());

    assertThrows(IllegalArgumentException.class,
        () -> MinioStagingUtil.stageFromMinio(minio, "empty/", tempDir, output));
  }
}
