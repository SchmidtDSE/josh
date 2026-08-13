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

const [warDir, ...jsDirs] = process.argv.slice(2);
const problems = [];

// ---- 1. The engine loads and runs -------------------------------------------------------------

const wasmPath = path.join(warDir, "wasm-gc", "JoshSim.wasm");
const runtimePath = path.join(warDir, "wasm-gc", "JoshSim.wasm-runtime.js");

let steps = 0;
const records = [];
let reported = null;
globalThis.reportStepComplete = () => { steps++; };
globalThis.reportData = (payload) => { records.push(payload); };
globalThis.reportError = (message) => { reported = message; };

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
      layer.exports.runSimulation(MODEL, SIMULATION, "", false, "");
      if (reported !== null) {
        problems.push(`running the check model reported: ${reported}`);
      } else if (records.length === 0) {
        // A valid module that runs but exports nothing means the memory:// write path is broken,
        // which is invisible to validation and fatal to every run button and the editor alike.
        problems.push("running the check model produced no output records");
      } else {
        console.log(`  engine ran the check model: ${steps} steps, ${records.length} records`);
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
