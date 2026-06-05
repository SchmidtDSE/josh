/**
 * Main entry point for the Josh introduction demo at demo.joshsim.org.
 *
 * Constructs and returns a NarrativePresenter which owns the full narrative flow.
 *
 * @license BSD-3-Clause
 */

import {NarrativePresenter} from "narrative";


/**
 * Initialize the demo narrative.
 *
 * @returns {NarrativePresenter} The presenter instance owning the narrative state.
 */
function main() {
  return new NarrativePresenter("narrative");
}


export {main};
