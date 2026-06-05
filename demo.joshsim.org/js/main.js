/**
 * Main entry point for the Josh introduction demo at demo.joshsim.org.
 *
 * Constructs a NarrativePresenter and a PlaygroundPresenter, wires them together, and starts
 * the narrative flow.
 *
 * @license BSD-3-Clause
 */

import {NarrativePresenter, FOREVERTREE_WASM_SNAPSHOT} from "narrative";
import {PlaygroundPresenter} from "playground";


/**
 * Initialize the demo narrative.
 *
 * @returns {NarrativePresenter} The presenter instance owning the narrative state.
 */
function main() {
  const narrativePresenter = new NarrativePresenter("narrative");

  const playgroundPresenter = new PlaygroundPresenter(
    "playground-editor",
    "playground-config",
    FOREVERTREE_WASM_SNAPSHOT,
    "data/forevertree.jshc"
  );

  narrativePresenter.setPlaygroundPresenter(playgroundPresenter);

  return narrativePresenter;
}


export {main};
