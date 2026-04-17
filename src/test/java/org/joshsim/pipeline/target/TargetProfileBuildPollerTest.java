/**
 * Tests for TargetProfile.buildPollingStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for {@link TargetProfile#buildPollingStrategy}.
 */
class TargetProfileBuildPollerTest {

  @TempDir
  File tempDir;

  @Test
  void httpTargetReturnsMinioPollingStrategy() throws Exception {
    TargetProfile profile = spy(loadProfile("http-target", """
        {
          "type": "http",
          "http": {
            "endpoint": "https://example.com",
            "apiKey": "key"
          },
          "minio_endpoint": "https://storage.example.com",
          "minio_access_key": "a",
          "minio_secret_key": "s",
          "minio_bucket": "b"
        }
        """));

    MinioHandler mockHandler = mock(MinioHandler.class);
    doReturn(mockHandler).when(profile).buildMinioHandler(
        any(OutputOptions.class)
    );

    BatchPollingStrategy strategy = profile.buildPollingStrategy(
        new OutputOptions()
    );

    assertTrue(strategy instanceof MinioPollingStrategy);
  }

  @Test
  void unsupportedTypeThrowsException() throws Exception {
    TargetProfile profile = loadProfile("bad-target", """
        {
          "type": "ssh",
          "minio_endpoint": "https://storage.example.com",
          "minio_access_key": "a",
          "minio_secret_key": "s",
          "minio_bucket": "b"
        }
        """);

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> profile.buildPollingStrategy(new OutputOptions())
    );
    assertTrue(ex.getMessage().contains("ssh"));
    assertTrue(
        ex.getMessage().contains("Unsupported target type")
    );
  }

  private TargetProfile loadProfile(
      String name,
      String json
  ) throws IOException {
    Files.writeString(
        new File(tempDir, name + ".json").toPath(), json
    );
    TargetProfileLoader loader =
        new TargetProfileLoader(tempDir);
    return loader.load(name);
  }
}
