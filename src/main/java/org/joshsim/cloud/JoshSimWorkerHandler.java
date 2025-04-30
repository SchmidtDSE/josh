/**
 * Handler for individual simulation replicate tasks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.util.Optional;
import org.geotools.api.referencing.FactoryException;
import org.geotools.referencing.CRS;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.SandboxInputOutputLayer;
import org.joshsim.lang.parse.ParseResult;


/**
 * Handler logic which executes individual simulation replicates and responds via HTTP 2 streaming.
 */
public class JoshSimWorkerHandler implements HttpHandler {

  private final CloudApiDataLayer apiDataLayer;
  private final EngineGeometryFactory geometryFactory;

  /**
   * Create a new handler for the leader node which is invoked via an HTTP 2 request.
   */
  public JoshSimWorkerHandler(CloudApiDataLayer apiInternalLayer, boolean sandboxed,
                              Optional<String> crs) {
    this.apiDataLayer = apiInternalLayer;
    
    if (!sandboxed) {
      throw new RuntimeException("Only sandboxed mode is supported at this time.");
    }

    if (crs.isPresent()) {
      try {
        geometryFactory = new EarthGeometryFactory(CRS.decode(crs.get()));
      } catch (FactoryException e) {
        throw new RuntimeException(e);
      }
    } else {
      geometryFactory = new GridGeometryFactory();
    }
  }

  /**
   * Simulate a single replicate and stream the results via HTTP 2.
   *
   * <p>Execute a single replicate using the form-encoded code found in the code input on the
   * request and stream any results back. This acts like a facade in that it uses JoshSimFacadeUtil
   * to interpret the program (returning an invalid user input status code and error message if an
   * issue was encountered) and runs the simulation with patches processed in parallel. The code
   * for the simulation is read from form-encoded code field and the simulation name from the name
   * form-encoded field.</p>
   * 
   * @param httpServerExchange The exchange through which this request should execute.
   */
  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    String apiKey = httpServerExchange.getRequestHeaders().get("api-key").getFirst();

    if (apiKey == null || !apiDataLayer.apiKeyIsValid(apiKey)) {
      httpServerExchange.setStatusCode(401);
      return;
    }

    long startTime = System.nanoTime();
    handleRequestTrusted(httpServerExchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiDataLayer.log(apiKey, "simulate", runtimeSeconds);
  }

  /**
   * Execute a request without interacting with the API service internals.
   * 
   * <p>Execute a request without interactingw ith the API service inernals as described in
   * handleRequest which checks the API key and reports logging.</p>
   * 
   * @param httpServerExchange The exchange through which this request should execute.
   */
  public void handleRequestTrusted(HttpServerExchange httpServerExchange) {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return;
    }

    httpServerExchange.setStatusCode(200);
    httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
    httpServerExchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    FormData formData = parser.parseBlocking();
    
    if (!formData.contains("code") || !formData.contains("name")) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    String code = formData.getFirst("code").getValue();
    String simulationName = formData.getFirst("name").getValue();

    if (code == null || simulationName == null) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      httpServerExchange.setStatusCode(400);
      httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
      httpServerExchange.getResponseSender().send(result.getErrors().iterator().next().toString());
      return;
    }

    JoshProgram program = JoshSimFacadeUtil.interpret(geometryFactory, result);
    if (!program.getSimulations().hasPrototype(simulationName)) {
      httpServerExchange.setStatusCode(404);
      return;
    }

    InputOutputLayer layer = getLayer(httpServerExchange);
    JoshSimFacadeUtil.runSimulation(
        geometryFactory,
        layer,
        program,
        simulationName,
        (step) -> {}, // No step reporting needed for worker
        true // Use parallel processing
    );
  }

  /**
   * Create a new input / output layer that writes exports to an exchange.
   * 
   * @param httpServerExchange The exchange where the response should be streamed.
   * @return The newly created layer.
   */
  private InputOutputLayer getLayer(HttpServerExchange httpServerExchange) {
    return new SandboxInputOutputLayer((export) -> {
      try {
        httpServerExchange.getOutputStream().write((export + "\n").getBytes());
        httpServerExchange.getOutputStream().flush();
      } catch (IOException e) {
        throw new RuntimeException("Error streaming response", e);
      }
    });
  }

}

