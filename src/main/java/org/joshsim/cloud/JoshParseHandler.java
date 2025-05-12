
/**
 * Handler for parsing Josh code and returning simulation names.
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
import java.util.stream.Collectors;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.parse.ParseResult;

/**
 * Handler which parses Josh code and returns parse results with simulation names.
 */
public class JoshParseHandler implements HttpHandler {

  private final CloudApiDataLayer apiDataLayer;
  private final EngineGeometryFactory geometryFactory;

  /**
   * Constructs a new JoshParseHandler.
   *
   * @param apiInternalLayer The cloud API data layer utilized by this handler for API operations.
   * @param sandboxed A boolean flag indicating whether to operate in sandboxed mode.
   * @param crs An optional string representing the coordinate reference system to be used.
   * @param serialPatches Flag indicating if patches should be processed in serial.
   */
  public JoshParseHandler(CloudApiDataLayer apiInternalLayer, boolean sandboxed,
      Optional<String> crs, boolean serialPatches) {
    this.apiDataLayer = apiInternalLayer;
    
    if (!sandboxed) {
      throw new RuntimeException("Only sandboxed mode is supported at this time.");
    }

    if (crs.isPresent()) {
      try {
        geometryFactory = new EarthGeometryFactory(CRS.forCode(crs.get()));
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse CRS: " + e);
      }
    } else {
      geometryFactory = new GridGeometryFactory();
    }
  }

  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    if (httpServerExchange.isInIoThread()) {
      httpServerExchange.dispatch(this);
      return;
    }

    if (!CorsUtil.addCorsHeaders(httpServerExchange)) {
      return;
    }

    long startTime = System.nanoTime();
    Optional<String> apiKey = handleRequestTrusted(httpServerExchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiDataLayer.log(apiKey.orElse(""), "parse", runtimeSeconds);
  }

  /**
   * Handle a parse request without interacting with API service internals.
   *
   * @param httpServerExchange The exchange through which this request should execute.
   * @return The API key used in the request or empty string if rejected.
   */
  public Optional<String> handleRequestTrusted(HttpServerExchange httpServerExchange) {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return Optional.empty();
    }

    httpServerExchange.setStatusCode(200);
    httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
    httpServerExchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return Optional.empty();
    }

    FormData formData = null;
    try {
      formData = parser.parseBlocking();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ApiKeyUtil.ApiCheckResult apiCheckResult = ApiKeyUtil.checkApiKey(formData, apiDataLayer);
    if (!apiCheckResult.getKeyIsValid()) {
      httpServerExchange.setStatusCode(401);
      return Optional.empty();
    }
    String apiKey = apiCheckResult.getApiKey();

    if (!formData.contains("code")) {
      httpServerExchange.setStatusCode(400);
      return Optional.of(apiKey);
    }

    String code = formData.getFirst("code").getValue();
    ParseResult result = JoshSimFacadeUtil.parse(code);
    
    String parseStatus = result.hasErrors() ? 
        result.getErrors().iterator().next().toString() : 
        "success";
    
    String simulations = result.hasErrors() ? "" :
        JoshSimFacadeUtil.interpret(geometryFactory, result)
            .getSimulations()
            .getSimulations()
            .stream()
            .collect(Collectors.joining(","));

    httpServerExchange.getResponseSender().send(parseStatus + "\t" + simulations);
    return Optional.of(apiKey);
  }
}
