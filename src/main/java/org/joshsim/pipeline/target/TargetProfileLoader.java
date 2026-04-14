/**
 * Loads target profiles from the user's configuration directory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;


/**
 * Loads {@link TargetProfile} instances from JSON files in the targets directory.
 *
 * <p>By default, profiles are read from {@code ~/.josh/targets/<name>.json}.
 * The directory path can be overridden for testing or custom deployments.</p>
 */
public class TargetProfileLoader {

  private static final String DEFAULT_TARGETS_DIR =
      System.getProperty("user.home") + File.separator + ".josh" + File.separator + "targets";

  private final File targetsDir;
  private final ObjectMapper mapper;

  /**
   * Constructs a loader that reads from the default targets directory.
   */
  public TargetProfileLoader() {
    this(new File(DEFAULT_TARGETS_DIR));
  }

  /**
   * Constructs a loader that reads from the specified targets directory.
   *
   * @param targetsDir The directory containing target profile JSON files.
   */
  public TargetProfileLoader(File targetsDir) {
    this.targetsDir = targetsDir;
    this.mapper = new ObjectMapper();
  }

  /**
   * Loads a target profile by name.
   *
   * <p>Reads and parses {@code <targetsDir>/<name>.json} into a {@link TargetProfile}.
   * Validates that the profile has a non-null type and the corresponding config block.</p>
   *
   * @param name The target name (corresponds to the JSON filename without extension).
   * @return The parsed and validated target profile.
   * @throws IOException If the file cannot be read or parsed.
   * @throws IllegalArgumentException If the profile is invalid (missing type or config).
   */
  public TargetProfile load(String name) throws IOException {
    File profileFile = new File(targetsDir, name + ".json");

    if (!profileFile.exists()) {
      throw new IOException(
          "Target profile not found: " + profileFile.getAbsolutePath()
      );
    }

    TargetProfile profile = mapper.readValue(profileFile, TargetProfile.class);
    profile.setSourceFilePath(profileFile.getAbsolutePath());
    validate(name, profile);
    return profile;
  }

  private void validate(String name, TargetProfile profile) {
    if (profile.getType() == null || profile.getType().isEmpty()) {
      throw new IllegalArgumentException(
          "Target profile '" + name + "' is missing required 'type' field"
      );
    }

    String type = profile.getType();
    if ("http".equals(type) && profile.getHttpConfig() == null) {
      throw new IllegalArgumentException(
          "Target profile '" + name + "' has type 'http' but is missing 'http' config block"
      );
    }

    if ("kubernetes".equals(type) && profile.getKubernetesConfig() == null) {
      throw new IllegalArgumentException(
          "Target profile '" + name + "' has type 'kubernetes' "
              + "but is missing 'kubernetes' config block"
      );
    }

    if ("kubernetes".equals(type) && profile.getKubernetesConfig() != null) {
      String podEndpoint =
          profile.getKubernetesConfig().getPodMinioEndpoint();
      if (podEndpoint == null || podEndpoint.isEmpty()) {
        throw new IllegalArgumentException(
            "Target profile '" + name + "' has type 'kubernetes'"
                + " but is missing required 'pod_minio_endpoint'"
                + " in the 'kubernetes' config block. Pods need"
                + " an explicit MinIO endpoint (may differ from"
                + " the host endpoint)."
        );
      }
    }
  }
}
