package org.joshsim.cloud;
import io.undertow.server.HttpServerExchange;


/**
 * Handler which calls a JoshSimServer via HTTP 2 to run individual jobs.
 *
 * <p>Handler which calls a JoshSimServer via HTTP to run individual jobs (JoshSimWorkerHandler),
 * sending results from different replicates streamed from HTTP 2 back to the original requester
 * through HTTP 2.</p>
 */
public class JoshSimLeaderHandler {

  private final CloudApiDataLayer apiInternalLayer;
  private final String urlToWorker;
  private final int maxParallelRequests;

  /**
   * Create a new leader handler.
   *
   * @param apiInternalLayer The facade to use for checking that API keys are valid before sending
   *     requests to worker with the user-provided API key and the facade in which logging should be
   *     executed.
   * @param urlToWorker String URL at which the worker requests should be executed. This may route
   *     to the same machine or to a different  machine.
   */
  public JoshSimLeaderHandler(CloudApiDataLayer apiInternalLayer, String urlToWorker,
        int maxParallelRequests) {
    this.apiInternalLayer = apiInternalLayer;
    this.urlToWorker = urlToWorker;
    this.maxParallelRequests = maxParallelRequests;
  }

  /**
   * Call up to maxParallelRequests to execute on workers.
   *
   * <p>Execute replicates in parallel by calling the worker URL multiple times, forwarding on the
   * API key, simulation, and simulation name found in the form-encoded body. This will stream
   * results back, splitting results recieved from workers per line and prepending the replicate
   * number to the string being returned such that there is a replicate number per line between
   * brackets like [1] for the second replicate. The prepended string is then streamed back to the
   * requester such that lines remain intact but those complete lines may be interleaved between
   * replicates. No more than maxParallelRequests will be executed at a time with the number of
   * replicates found on the form-encoded body in parameter name replicates.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   */
  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {}

  /**
   * Execute a request without interacting with the API service internals.
   *
   * <p>Execute a request without interacting with the API service inernals as described in
   * handleRequest which checks the API key and reports logging.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   */
  public void handleRequestTrusted(HttpServerExchange httpServerExchange) {}
  
}
