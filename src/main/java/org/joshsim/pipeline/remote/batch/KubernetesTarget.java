/**
 * Kubernetes implementation of RemoteBatchTarget.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.joshsim.util.OutputOptions;


/**
 * Submits and manages batch jobs on a Kubernetes cluster via kubectl.
 *
 * <p>Uses kubectl CLI commands via ProcessBuilder for all cluster interactions:
 * job submission (kubectl apply), status polling (kubectl get), log retrieval
 * (kubectl logs), and cleanup (kubectl delete). Renders indexed Job YAML from
 * a template with completionMode: Indexed for replicate-level parallelism.</p>
 */
public class KubernetesTarget implements RemoteBatchTarget {

  private static final long KUBECTL_TIMEOUT_SECONDS = 30;
  private static final String JOB_NAME_PREFIX = "josh-sim-";

  private final KubernetesConfig config;
  private final OutputOptions output;

  /**
   * Creates a new KubernetesTarget.
   *
   * @param config the Kubernetes cluster configuration
   * @param output for logging
   */
  public KubernetesTarget(KubernetesConfig config, OutputOptions output) {
    this.config = config;
    this.output = output;
  }

  @Override
  public String submitJob(BatchJobSpec spec) throws IOException {
    String jobName = JOB_NAME_PREFIX + spec.getJobId();
    String yaml = renderJobYaml(spec, jobName);

    output.printInfo("Submitting K8s Job: " + jobName + " (" + spec.getTotalReplicates()
        + " replicates, parallelism=" + spec.getMaxParallelism() + ")");

    List<String> cmd = buildKubectlCommand("apply", "-f", "-");
    ProcessResult result = executeWithStdin(cmd, yaml);

    if (result.exitCode != 0) {
      throw new IOException("kubectl apply failed (exit " + result.exitCode + "): "
          + result.stderr);
    }

    output.printInfo("Job submitted: " + jobName);
    return spec.getJobId();
  }

  @Override
  public JobStatus pollStatus(String jobId) throws IOException {
    String jobName = JOB_NAME_PREFIX + jobId;

    List<String> cmd = buildKubectlCommand(
        "get", "job", jobName,
        "-o", "jsonpath={.status.succeeded},{.status.failed},{.status.active},{.spec.completions}"
    );
    ProcessResult result = execute(cmd);

    if (result.exitCode != 0) {
      if (result.stderr.contains("not found")) {
        return JobStatus.FAILED;
      }
      throw new IOException("kubectl get job failed: " + result.stderr);
    }

    return parseJobStatus(result.stdout.trim());
  }

  @Override
  public String getLogs(String jobId) throws IOException {
    String jobName = JOB_NAME_PREFIX + jobId;

    List<String> cmd = buildKubectlCommand(
        "logs", "-l", "job-name=" + jobName, "--prefix", "--tail=200"
    );
    ProcessResult result = execute(cmd);

    if (result.exitCode != 0) {
      return "Failed to retrieve logs: " + result.stderr;
    }

    return result.stdout;
  }

  @Override
  public void cleanup(String jobId) throws IOException {
    String jobName = JOB_NAME_PREFIX + jobId;
    output.printInfo("Cleaning up K8s Job: " + jobName);

    List<String> cmd = buildKubectlCommand(
        "delete", "job", jobName, "--ignore-not-found"
    );
    ProcessResult result = execute(cmd);

    if (result.exitCode != 0) {
      output.printError("Warning: cleanup failed for " + jobName + ": " + result.stderr);
    }
  }

  /**
   * Renders the K8s Job YAML for an indexed completion job.
   *
   * @param spec the batch job specification
   * @param jobName the K8s job name
   * @return the rendered YAML string
   */
  String renderJobYaml(BatchJobSpec spec, String jobName) {
    final int parallelism = spec.getMaxParallelism() > 0
        ? spec.getMaxParallelism()
        : spec.getJobCount();

    StringBuilder yaml = new StringBuilder();
    yaml.append("apiVersion: batch/v1\n");
    yaml.append("kind: Job\n");
    yaml.append("metadata:\n");
    yaml.append("  name: ").append(jobName).append("\n");
    yaml.append("  namespace: ").append(config.getNamespace()).append("\n");
    yaml.append("spec:\n");
    yaml.append("  completionMode: Indexed\n");
    yaml.append("  completions: ").append(spec.getJobCount()).append("\n");
    yaml.append("  parallelism: ").append(parallelism).append("\n");
    yaml.append("  backoffLimit: ").append(config.getBackoffLimit()).append("\n");
    yaml.append("  activeDeadlineSeconds: ").append(spec.getTimeoutSeconds()).append("\n");
    yaml.append("  ttlSecondsAfterFinished: 3600\n");
    yaml.append("  template:\n");
    yaml.append("    spec:\n");

    // Node selector
    Map<String, String> nodeSelector = config.getNodeSelector();
    if (!nodeSelector.isEmpty()) {
      yaml.append("      nodeSelector:\n");
      for (Map.Entry<String, String> entry : nodeSelector.entrySet()) {
        yaml.append("        ").append(entry.getKey()).append(": \"")
            .append(entry.getValue()).append("\"\n");
      }
    }

    yaml.append("      containers:\n");
    yaml.append("      - name: josh-worker\n");
    yaml.append("        image: ").append(spec.getImage()).append("\n");
    yaml.append("        command:\n");
    yaml.append("        - java\n");

    // Compute Xmx as ~80% of requested memory
    String xmx = computeXmx(spec.getMemory());
    yaml.append("        - \"-Xmx").append(xmx).append("\"\n");
    yaml.append("        - -jar\n");
    yaml.append("        - joshsim-fat.jar\n");
    yaml.append("        - runFromMinio\n");
    yaml.append("        - \"--input-prefix=").append(spec.getInputPrefix()).append("\"\n");
    yaml.append("        - \"--simulation=").append(spec.getSimulation()).append("\"\n");
    yaml.append("        - \"--replicate-id=$(JOB_COMPLETION_INDEX)\"\n");

    // MinIO options
    if (spec.getMinioOptions() != null && spec.getMinioOptions().isMinioOutput()) {
      yaml.append("        - \"--minio-endpoint=")
          .append(spec.getMinioOptions().getMinioEndpoint()).append("\"\n");
      yaml.append("        - \"--minio-bucket=")
          .append(spec.getMinioOptions().getBucketName()).append("\"\n");
    }

    if (spec.isUseFloat64()) {
      yaml.append("        - --use-float-64\n");
    }

    // Environment variables
    yaml.append("        env:\n");
    yaml.append("        - name: JOB_COMPLETION_INDEX\n");
    yaml.append("          valueFrom:\n");
    yaml.append("            fieldRef:\n");
    yaml.append("              fieldPath: metadata.annotations");
    yaml.append("['batch.kubernetes.io/job-completion-index']\n");

    // MinIO credentials as env vars
    for (Map.Entry<String, String> entry : spec.getEnv().entrySet()) {
      yaml.append("        - name: ").append(entry.getKey()).append("\n");
      yaml.append("          value: \"").append(entry.getValue()).append("\"\n");
    }

    // Resources
    yaml.append("        resources:\n");
    yaml.append("          requests:\n");
    yaml.append("            memory: \"").append(spec.getMemory()).append("\"\n");
    yaml.append("            cpu: \"").append(spec.getCpu()).append("\"\n");
    if (spec.getGpu() > 0) {
      yaml.append("            nvidia.com/gpu: \"").append(spec.getGpu()).append("\"\n");
    }
    yaml.append("          limits:\n");
    yaml.append("            memory: \"").append(spec.getMemory()).append("\"\n");
    yaml.append("            cpu: \"").append(spec.getCpu()).append("\"\n");
    if (spec.getGpu() > 0) {
      yaml.append("            nvidia.com/gpu: \"").append(spec.getGpu()).append("\"\n");
    }

    yaml.append("      restartPolicy: Never\n");

    return yaml.toString();
  }

  /**
   * Parses kubectl jsonpath output into a JobStatus.
   *
   * <p>The format is: succeeded,failed,active,completions (any may be empty).</p>
   *
   * @param statusStr the jsonpath output
   * @return the parsed job status
   */
  JobStatus parseJobStatus(String statusStr) {
    String[] parts = statusStr.split(",", -1);
    if (parts.length < 4) {
      return JobStatus.PENDING;
    }

    int succeeded = parseIntOrZero(parts[0]);
    int failed = parseIntOrZero(parts[1]);
    int active = parseIntOrZero(parts[2]);
    int completions = parseIntOrZero(parts[3]);

    if (failed > 0) {
      return JobStatus.FAILED;
    }
    if (completions > 0 && succeeded >= completions) {
      return JobStatus.COMPLETE;
    }
    if (active > 0 || succeeded > 0) {
      return JobStatus.RUNNING;
    }
    return JobStatus.PENDING;
  }

  /**
   * Builds a kubectl command with common flags (context, namespace).
   *
   * @param args the kubectl subcommand and arguments
   * @return the full command list
   */
  List<String> buildKubectlCommand(String... args) {
    List<String> cmd = new ArrayList<>();
    cmd.add("kubectl");

    if (config.getContext() != null && !config.getContext().isEmpty()) {
      cmd.add("--context=" + config.getContext());
    }

    cmd.add("--namespace=" + config.getNamespace());

    for (String arg : args) {
      cmd.add(arg);
    }

    return cmd;
  }

  /**
   * Executes a command and captures output.
   *
   * @param command the command to execute
   * @return the process result
   * @throws IOException if the process cannot be started
   */
  ProcessResult execute(List<String> command) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    Process process = pb.start();

    String stdout = readStream(process.getInputStream());
    String stderr = readStream(process.getErrorStream());

    try {
      boolean finished = process.waitFor(KUBECTL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IOException("kubectl timed out after " + KUBECTL_TIMEOUT_SECONDS + "s");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("kubectl interrupted", e);
    }

    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  /**
   * Executes a command with stdin data and captures output.
   *
   * @param command the command to execute
   * @param stdin the data to write to stdin
   * @return the process result
   * @throws IOException if the process cannot be started
   */
  ProcessResult executeWithStdin(List<String> command, String stdin) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    Process process = pb.start();

    process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
    process.getOutputStream().close();

    String stdout = readStream(process.getInputStream());
    String stderr = readStream(process.getErrorStream());

    try {
      boolean finished = process.waitFor(KUBECTL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IOException("kubectl timed out after " + KUBECTL_TIMEOUT_SECONDS + "s");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("kubectl interrupted", e);
    }

    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  private String readStream(java.io.InputStream stream) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Computes -Xmx value as ~80% of the memory request.
   *
   * @param memory the K8s memory request (e.g., "256Gi", "8Gi", "4096Mi")
   * @return the Xmx string (e.g., "200g", "6g", "3276m")
   */
  static String computeXmx(String memory) {
    if (memory == null || memory.isEmpty()) {
      return "4g";
    }

    String lower = memory.toLowerCase();
    try {
      if (lower.endsWith("gi")) {
        int gi = Integer.parseInt(lower.substring(0, lower.length() - 2));
        return (int) (gi * 0.8) + "g";
      } else if (lower.endsWith("mi")) {
        int mi = Integer.parseInt(lower.substring(0, lower.length() - 2));
        return (int) (mi * 0.8) + "m";
      }
    } catch (NumberFormatException e) {
      // Fall through to default
    }
    return "4g";
  }

  private static int parseIntOrZero(String str) {
    if (str == null || str.isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseInt(str.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Result of a process execution.
   */
  static class ProcessResult {
    final int exitCode;
    final String stdout;
    final String stderr;

    ProcessResult(int exitCode, String stdout, String stderr) {
      this.exitCode = exitCode;
      this.stdout = stdout;
      this.stderr = stderr;
    }
  }
}
