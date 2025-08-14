/**
 * Handler for discovering configuration variables in Josh code.
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
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.visitor.JoshConfigDiscoveryVisitor;


/**
 * Handler which parses Josh code and returns discovered configuration variables.
 *
 * <p>This handler accepts POST requests with Josh source code and analyzes it to
 * discover all configuration variables referenced using 'config' expressions.
 * Results are returned as a comma-separated string of variable names.</p>
 */
public class JoshConfigDiscoveryHandler implements HttpHandler {

  private final CloudApiDataLayer apiDataLayer;

  /**
   * Constructs a new JoshConfigDiscoveryHandler.
   *
   * @param apiInternalLayer The cloud API data layer utilized by this handler for API operations.
   */
  public JoshConfigDiscoveryHandler(CloudApiDataLayer apiInternalLayer) {
    this.apiDataLayer = apiInternalLayer;
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
    apiDataLayer.log(apiKey.orElse(""), "discoverConfig", runtimeSeconds);
  }

  /**
   * Handle a config discovery request without interacting with API service internals.
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
      httpServerExchange.getResponseSender().send("Missing 'code' parameter");
      return Optional.of(apiKey);
    }

    String code = formData.getFirst("code").getValue();

    try {
      // Parse Josh code to discover config variables
      JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(code));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      JoshLangParser joshParser = new JoshLangParser(tokens);
      ParseTree tree = joshParser.program();

      JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
      visitor.visit(tree);
      Set<String> configVariables = visitor.getDiscoveredVariables();

      // Convert to comma-separated string
      String result = configVariables.stream()
          .collect(Collectors.joining(","));

      httpServerExchange.getResponseSender().send(result);
      return Optional.of(apiKey);

    } catch (Exception e) {
      httpServerExchange.setStatusCode(500);
      httpServerExchange.getResponseSender().send("Error parsing code: " + e.getMessage());
      return Optional.of(apiKey);
    }
  }
}