/**
 * Entry point for the ForeverTree *management* extension demo at demo.joshsim.org/management.
 *
 * Reuses the shared NarrativePresenter and PlaygroundPresenter, injecting the management step list,
 * the four external data layers (climate + the burn/managed masks), the scenario config, and the
 * richer set of result variables. The page lives one directory deep, so shared assets are reached
 * via "../" and Ace's base path is "../third_party".
 *
 * @license BSD-3-Clause
 */

import {NarrativePresenter} from "narrative";
import {PlaygroundPresenter} from "playground";
import {MANAGEMENT_STEPS, MANAGEMENT_WASM_SNAPSHOT} from "management-steps";


/**
 * Initialize the management narrative.
 *
 * @returns {NarrativePresenter} The presenter instance owning the narrative state.
 */
function main() {
  const narrativePresenter = new NarrativePresenter("narrative", MANAGEMENT_STEPS);

  const playgroundPresenter = new PlaygroundPresenter(
    "playground-editor",
    "playground-config",
    MANAGEMENT_WASM_SNAPSHOT,
    "data/scenario.jshc",
    {
      dataManifest: {
        "temperature.jshd": "data/temperature.jshd",
        "precipitation.jshd": "data/precipitation.jshd",
        "burned.jshd": "data/burned.jshd",
        "managed.jshd": "data/managed.jshd",
      },
      configKey: "scenario.jshc",
      aceBasePath: "../third_party",
      resultVariables: [
        {key: "meanHeight", reducer: "mean", label: "Mean tree height"},
        {key: "invasiveCover", reducer: "mean", label: "Invasive grass cover"},
        {key: "nLiveTrees", reducer: "mean", label: "Live trees"},
        {key: "nBurned", reducer: "mean", label: "Burned trees"},
        {key: "nJuvenile", reducer: "mean", label: "Juvenile trees"},
        {key: "nAdult", reducer: "mean", label: "Adult trees"},
      ],
    }
  );

  narrativePresenter.setPlaygroundPresenter(playgroundPresenter);

  return narrativePresenter;
}


export {main};
