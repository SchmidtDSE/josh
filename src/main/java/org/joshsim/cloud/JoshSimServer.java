/**
 * Small server which can serve the local editor and run simulations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;


/**
 * Undertow server running either Josh Cloud or a self-hosted cloud.
 *
 * <p>Undertow server running either Josh Cloud or a self-hosted cloud which has endpoints for both
 * the worker and leader operations as well as static serving of the editor at the root and a health
 * endpoint which simply responds with 200 healthy. The endpoint for the worker is /runSimulation,
 * the endpoint for the leader is /runReplicates, and the endpoint for health is /health. Runs
 * using HTTP 2.</p>
 */
public class JoshSimServer {}
