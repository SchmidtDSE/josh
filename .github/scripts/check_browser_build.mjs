/**
 * Check that the browser build actually works, and that its JavaScript stays widely compatible.
 *
 * This exists because two failures shipped silently and sat in production. The WasmGC module stopped
 * validating in January and no one noticed for eight months, because `wasm.worker.js` catches the
 * load failure and quietly falls back to the JavaScript backend. Separately, a `Map.keys().forEach`
 * call reached the live editor and demo, where it throws on any engine without iterator helpers.
 * Nothing in CI ran, linted, or loaded a single line of browser code, so neither had anywhere to
 * surface.
 *
 * Two checks, aimed at those two failures:
 *
 *   1. Load the WebAssembly module and run a real model through it. Validation alone would have
 *      caught the January regression, but running is what proves the engine works rather than
 *      merely parses -- a module can be valid and still throw on its first call.
 *   2. Scan the browser JavaScript for APIs newer than the baseline this project supports. The
 *      scan is a denylist and so is not exhaustive; it is meant to catch the specific class of
 *      mistake that is invisible on a modern developer machine and fatal on a user's browser.
 *
 * Usage: node check_browser_build.mjs <war-dir> [js-dir ...]
 */

import fs from "node:fs";
import path from "node:path";

/**
 * APIs newer than the baseline, with the reason each is out.
 *
 * The baseline is deliberately conservative: this is documentation a reader runs, so it should work
 * on whatever browser they already have rather than the one a maintainer happens to use.
 */
const TOO_NEW = [
  [/(?:keys|values|entries)\(\)\s*\.\s*(?:map|filter|forEach|reduce|some|every|find|toArray|take|drop|flatMap)\(/,
    "iterator helper on the iterator returned by keys()/values()/entries() -- call the method on the collection instead"],
  [/\.toSorted\(|\.toReversed\(|\.toSpliced\(/, "ES2023 immutable array method"],
  [/Object\.groupBy|Map\.groupBy/, "ES2024 groupBy"],
  [/Promise\.withResolvers/, "ES2024 Promise.withResolvers"],
  [/Array\.fromAsync/, "ES2024 Array.fromAsync"],
  [/\bstructuredClone\(/, "structuredClone"],
];

/**
 * The model the engine is asked to run.
 *
 * Inline rather than a path into the repository, deliberately. Every real model here is documented
 * content that moves as the docs tree is reorganized, and a check that breaks when a guide is
 * renamed teaches people to weaken it. This exercises the whole contract the browser depends on --
 * parse, interpret, step, and write to a `memory://` target -- in a few lines that answer to nobody.
 */
const MODEL = `start simulation Main

  grid.size = 1000 m
  grid.low = 0 m latitude, 0 m longitude
  grid.high = 3000 m latitude, 3000 m longitude
  grid.patch = "Default"

  steps.low = 0 count
  steps.high = 2 count

  exportFiles.patch = "memory://editor/patches"

end simulation

start patch Default

  count.init = 1 count
  count.step = prior.count + 1 count

  export.total.step = count

end patch
`;

const SIMULATION = "Main";

/**
 * A model whose output depends entirely on the random draw.
 *
 * The check model above is deterministic, so it runs identically seeded or not and says nothing
 * about whether the seed arrived. This one exports nothing but the draw, which makes "same seed,
 * same answer" and "different seed, different answer" both observable in the exported records.
 */
const STOCHASTIC_MODEL = `start simulation Seeded

  grid.size = 1000 m
  grid.low = 0 m latitude, 0 m longitude
  grid.high = 3000 m latitude, 3000 m longitude
  grid.patch = "Default"

  steps.low = 0 count
  steps.high = 2 count

  exportFiles.patch = "memory://editor/patches"

end simulation

start patch Default

  draw.init = sample uniform from 0 count to 1000000 count
  draw.step = sample uniform from 0 count to 1000000 count

  export.draw.step = draw

end patch
`;

const STOCHASTIC_SIMULATION = "Seeded";

const [warDir, ...jsDirs] = process.argv.slice(2);
const problems = [];

// ---- 1. The engine loads and runs -------------------------------------------------------------

const wasmPath = path.join(warDir, "wasm-gc", "JoshSim.wasm");
const runtimePath = path.join(warDir, "wasm-gc", "JoshSim.wasm-runtime.js");

let steps = 0;
let records = [];
let reported = null;
globalThis.reportStepComplete = () => { steps++; };
globalThis.reportData = (payload) => { records.push(payload); };
globalThis.reportError = (message) => { reported = message; };

/**
 * Run one model through the engine and collect what it exported.
 *
 * The engine reports results through globals rather than a return value, so each run has to start
 * from a clean slate. Errors are reported the same way -- `runSimulation` catches and calls
 * `reportError` -- which is why the reported message is returned rather than thrown.
 *
 * @param {object} layer - The loaded TeaVM WasmGC layer.
 * @param {string} model - The Josh source to run.
 * @param {string} simulation - The name of the simulation within that source.
 * @param {string} seed - Seed as a decimal string, or "" to run unseeded.
 * @returns {{error: ?string, steps: number, output: string}} What the run produced.
 */
function runModel(layer, model, simulation, seed) {
  steps = 0;
  records = [];
  reported = null;
  layer.exports.runSimulation(model, simulation, "", false, "", seed);
  return {error: reported, steps: steps, output: records.join("\n")};
}

try {
  await WebAssembly.compile(fs.readFileSync(wasmPath));
} catch (error) {
  // The failure mode this check was written for: the module is rejected whole, before one
  // instruction runs, and every browser silently uses the JavaScript backend instead.
  problems.push(`WebAssembly module does not validate: ${error.message}`);
}

if (problems.length === 0) {
  try {
    (0, eval)(fs.readFileSync(runtimePath, "utf8"));
    const layer = await TeaVM.wasmGC.load(wasmPath);
    const invalid = layer.exports.validate(MODEL);
    if (invalid !== "") {
      problems.push(`the engine rejected the check model: ${invalid}`);
    } else {
      const run = runModel(layer, MODEL, SIMULATION, "");
      if (run.error !== null) {
        problems.push(`running the check model reported: ${run.error}`);
      } else if (run.output === "") {
        // A valid module that runs but exports nothing means the memory:// write path is broken,
        // which is invisible to validation and fatal to every run button and the editor alike.
        problems.push("running the check model produced no output records");
      } else {
        console.log(`  engine ran the check model: ${run.steps} steps`);
      }
    }

    // ---- The seed reaches the engine ----------------------------------------------------------
    //
    // A seed argument that is accepted and dropped looks exactly like one that works, because JS
    // ignores surplus arguments and every run still completes. Both directions are checked: equal
    // output under one seed is worthless without unequal output under another, since a broken run
    // that exports nothing would satisfy the first on its own.
    const invalidStochastic = layer.exports.validate(STOCHASTIC_MODEL);
    if (invalidStochastic !== "") {
      problems.push(`the engine rejected the seed check model: ${invalidStochastic}`);
    } else {
      const first = runModel(layer, STOCHASTIC_MODEL, STOCHASTIC_SIMULATION, "42");
      const again = runModel(layer, STOCHASTIC_MODEL, STOCHASTIC_SIMULATION, "42");
      const other = runModel(layer, STOCHASTIC_MODEL, STOCHASTIC_SIMULATION, "43");
      const failed = [first, again, other].find((result) => result.error !== null);

      if (failed !== undefined) {
        problems.push(`running the seed check model reported: ${failed.error}`);
      } else if (first.output === "") {
        problems.push("the seed check model exported nothing, so the seed cannot be observed");
      } else if (first.output !== again.output) {
        problems.push("the same seed produced different draws, so runs are not reproducible");
      } else if (first.output === other.output) {
        problems.push("a different seed produced identical draws, so the seed is being ignored");
      } else {
        console.log("  engine honored the seed: same seed repeats, different seed diverges");
      }
    }
  } catch (error) {
    problems.push(`the engine threw while running the check model: ${error.message}`);
  }
}

// ---- 2. The browser JavaScript stays within the baseline ---------------------------------------

const scanned = [];
for (const dir of jsDirs) {
  // A directory named but absent is reported rather than ignored or fatal: the browser trees differ
  // between branches, and a check that dies on the wrong branch gets removed rather than fixed.
  if (!fs.existsSync(dir)) {
    console.log(`  (skipped ${dir}: not present on this branch)`);
    continue;
  }
  scanned.push(dir);
  for (const entry of fs.readdirSync(dir)) {
    if (!entry.endsWith(".js")) {
      continue;
    }
    const file = path.join(dir, entry);
    const lines = fs.readFileSync(file, "utf8").split("\n");
    lines.forEach((line, index) => {
      // Naming an API in a comment is not calling it, and the comment explaining why something was
      // avoided is exactly where its name will appear. The test is deliberately shallow -- a line
      // that only starts a comment -- because this is a denylist scan, not a parser.
      const code = line.trim();
      if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) {
        return;
      }
      for (const [pattern, why] of TOO_NEW) {
        if (pattern.test(line)) {
          problems.push(`${file}:${index + 1}: ${why}`);
        }
      }
    });
  }
}

if (problems.length > 0) {
  console.error("\nBrowser build check failed:\n");
  for (const problem of problems) {
    console.error(`  ${problem}`);
  }
  process.exit(1);
}
console.log(`  browser JavaScript is within the baseline (${scanned.length} directories scanned)`);
