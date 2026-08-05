/**
 * Shared credential Secret handling for Kubernetes batch targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * Creates the per-job MinIO credential Secret and ties its lifetime to the Job.
 *
 * <p>Pods read MinIO credentials through {@code secretKeyRef} rather than plain
 * env vars, so each dispatch needs its own Secret. Creation happens in two
 * steps, in this order for a reason:</p>
 *
 * <ol>
 *   <li>{@link #create} writes the Secret <em>before</em> the Job exists, so no
 *       pod can ever start against a missing {@code secretKeyRef} and land in
 *       {@code CreateContainerConfigError}.</li>
 *   <li>{@link #bindToJob} adds an {@code ownerReferences} entry pointing at the
 *       created Job, which can only be done once the Job has a UID.</li>
 * </ol>
 *
 * <p>The owner reference is what makes cleanup happen: Kubernetes garbage
 * collection deletes the Secret when its owning Job is deleted, whether by
 * {@code ttlSecondsAfterFinished} or by hand. Without it every dispatch leaves
 * a Secret holding live object-store credentials in the namespace forever —
 * Job TTL reaps the Job and its pods, never the Secret.</p>
 *
 * <p>Shared by {@link KubernetesTarget} and {@link KubernetesPreprocessTarget}
 * so both dispatch paths clean up identically.</p>
 */
final class KubernetesJobSecret {

  private KubernetesJobSecret() {
  }

  /**
   * Creates the per-job credential Secret.
   *
   * <p>Call before creating the Job that consumes it.</p>
   *
   * @param client The Fabric8 Kubernetes client.
   * @param namespace The namespace to create the Secret in.
   * @param secretName The Secret name, unique per job.
   * @param credentials Plaintext key/value pairs; base64-encoded here.
   */
  static void create(
      KubernetesClient client,
      String namespace,
      String secretName,
      Map<String, String> credentials
  ) {
    Map<String, String> data = new HashMap<>();
    credentials.forEach((key, value) -> data.put(key, encode(value)));

    Secret secret = new SecretBuilder()
        .withNewMetadata()
            .withName(secretName)
            .withNamespace(namespace)
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build();

    client.secrets()
        .inNamespace(namespace)
        .resource(secret)
        .create();
  }

  /**
   * Makes the given Job the owner of an already-created Secret.
   *
   * <p>Failures are reported on stderr rather than thrown. By the time this
   * runs the Job has been accepted by the API server and pods may already be
   * running, so a namespace that withholds patch rights on Secrets should
   * degrade to the old leak-the-Secret behaviour instead of failing a dispatch
   * that otherwise succeeded.</p>
   *
   * @param client The Fabric8 Kubernetes client.
   * @param namespace The namespace holding both Secret and Job.
   * @param secretName The Secret name passed to {@link #create}.
   * @param createdJob The Job as returned by {@code create()}, carrying its UID.
   */
  static void bindToJob(
      KubernetesClient client,
      String namespace,
      String secretName,
      Job createdJob
  ) {
    if (createdJob == null || createdJob.getMetadata() == null
        || createdJob.getMetadata().getUid() == null
        || createdJob.getMetadata().getUid().isEmpty()) {
      warnUnbound(secretName, "the created Job reported no UID");
      return;
    }

    OwnerReference owner = new OwnerReferenceBuilder()
        .withApiVersion("batch/v1")
        .withKind("Job")
        .withName(createdJob.getMetadata().getName())
        .withUid(createdJob.getMetadata().getUid())
        .withController(true)
        // Deleting the Secret need not block deletion of the Job, and blocking
        // would additionally require finalizer rights on the Job.
        .withBlockOwnerDeletion(false)
        .build();

    try {
      client.secrets()
          .inNamespace(namespace)
          .withName(secretName)
          .edit(current -> new SecretBuilder(current)
              .editMetadata()
                  .addToOwnerReferences(owner)
              .endMetadata()
              .build());
    } catch (Exception e) {
      warnUnbound(secretName, e.getMessage());
    }
  }

  private static void warnUnbound(String secretName, String reason) {
    System.err.println(
        "WARNING: could not bind Secret " + secretName + " to its Job ("
        + reason + "). The Secret holds object-store credentials and will "
        + "not be garbage collected — delete it manually."
    );
  }

  private static String encode(String value) {
    return Base64.getEncoder().encodeToString(
        value.getBytes(StandardCharsets.UTF_8)
    );
  }
}
