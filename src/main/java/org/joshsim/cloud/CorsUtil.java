/**
 * Utilities to help with CORS requests.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;


/**
 * Utility functions to help with CORS requests.
 */
public class CorsUtil {

  /**
   * Add CORS headers indicating all actions available on resource.
   *
   * @param exchange The exchange in which to add the headers.
   * @return True if the request should continue or false if should return right away.
   */
  public static boolean addCorsHeaders(HttpServerExchange exchange) {
    exchange.getResponseHeaders().put(
        new HttpString("Access-Control-Allow-Origin"),
        "*"
    );
    exchange.getResponseHeaders().put(
        new HttpString("Access-Control-Allow-Methods"),
        "GET, POST, PUT, DELETE, OPTIONS"
    );
    exchange.getResponseHeaders().put(
        new HttpString("Access-Control-Allow-Headers"),
        "Content-Type, Authorization"
    );
    exchange.getResponseHeaders().put(
        new HttpString("Access-Control-Max-Age"),
        "3600"
    );

    if (exchange.getRequestMethod().toString().equals("OPTIONS")) {
      exchange.setStatusCode(200);
      exchange.endExchange();
      return false;
    } else {
      return true;
    }
  }

}
