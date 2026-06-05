/**
 * NarrativePresenter: manages the multi-phase narrative flow for the Josh introduction demo.
 *
 * Owns a flat ordered list of step descriptors. Index 0 is the Welcome phase; indices 1+ are
 * build-up substeps. Public API: goNext(), goPrev(), goTo(index), getCurrentIndex(),
 * getStepCount(). Component 4 extends _buildSteps() to append further substep descriptors.
 *
 * @license BSD-3-Clause
 */


/**
 * Manages the multi-phase introduction narrative for demo.joshsim.org.
 *
 * Each step is described by a plain-object descriptor. The presenter owns phase show/hide,
 * code-panel line-by-line diff rendering with per-line glow-then-settle animation, and nav button state.
 */
class NarrativePresenter {

  /**
   * Create a new NarrativePresenter and immediately set up event wiring.
   *
   * @param {string} rootId - The id of the root narrative container element.
   */
  constructor(rootId) {
    const self = this;
    self._root = document.getElementById(rootId);
    self._currentIndex = 0;
    self._steps = self._buildSteps();
    self._setup();
  }

  /**
   * Advance one step forward.
   */
  goNext() {
    const self = this;
    if (self._currentIndex < self._steps.length - 1) {
      const from = self._currentIndex;
      self._currentIndex += 1;
      self._render(from);
    }
  }

  /**
   * Retreat one step backward.
   */
  goPrev() {
    const self = this;
    if (self._currentIndex > 0) {
      const from = self._currentIndex;
      self._currentIndex -= 1;
      self._render(from);
    }
  }

  /**
   * Jump to an arbitrary step by index. Used by the table of contents (Component 9).
   *
   * @param {number} index - Zero-based step index to jump to.
   */
  goTo(index) {
    const self = this;
    if (index >= 0 && index < self._steps.length && index !== self._currentIndex) {
      const from = self._currentIndex;
      self._currentIndex = index;
      self._render(from);
    }
  }

  /**
   * Returns the current step index.
   *
   * @returns {number} Current zero-based step index.
   */
  getCurrentIndex() {
    return this._currentIndex;
  }

  /**
   * Returns the total number of registered steps.
   *
   * @returns {number} Total step count.
   */
  getStepCount() {
    return this._steps.length;
  }

  /**
   * Wire button event listeners, build initial step list, and render the initial step.
   */
  _setup() {
    const self = this;

    const welcomeNext = document.getElementById("welcome-next-button");
    if (welcomeNext) {
      welcomeNext.addEventListener("click", () => self.goNext());
    }

    const buildupPrev = document.getElementById("buildup-prev-button");
    if (buildupPrev) {
      buildupPrev.addEventListener("click", () => self.goPrev());
    }

    const buildupNext = document.getElementById("buildup-next-button");
    if (buildupNext) {
      buildupNext.addEventListener("click", () => self.goNext());
    }

    self._render(null);
  }

  /**
   * Build and return the ordered array of step descriptors.
   *
   * Index 0 is the Welcome step (no code). Indices 1+ are build-up substeps. Component 4 extends
   * this method to append the remaining substep descriptors.
   *
   * Each descriptor has the shape:
   *   {
   *     id:           string,   // unique identifier, e.g. "welcome" or "buildup-1-structure"
   *     kind:         string,   // "welcome" | "buildup" | "playground" | "conclusion"
   *     codeSnapshot: string[], // FULL accumulated code at this substep (lines), [] for non-buildup
   *     heading:      string,   // h2 text for the right panel (null for welcome)
   *     body:         string,   // explanatory paragraph HTML for the right panel (null for welcome)
   *   }
   *
   * @returns {Array<Object>} Ordered step descriptor array.
   */
  _buildSteps() {
    return [
      {
        id: "welcome",
        kind: "welcome",
        codeSnapshot: [],
        heading: null,
        body: null,
      },
      {
        id: "buildup-1-structure",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "end organism",
        ],
        heading: "Structure",
        body: "A Josh simulation is composed of three kinds of entities: a <strong>simulation</strong>,"
          + " which defines global settings and the simulation grid; a <strong>patch</strong>, which"
          + " represents a location on that grid; and an <strong>organism</strong>, which represents"
          + " an individual living within a patch. Here we define the skeleton for the ForeverTree"
          + " simulation — three empty blocks we will fill in over the next steps.",
      },
      {
        id: "buildup-2-geography",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "end organism",
        ],
        heading: "Geography",
        body: "Every Josh simulation runs on a spatial grid. The <code>grid.size</code> sets the"
          + " resolution of each patch — here 16 km × 16 km cells. <code>grid.top_left</code> and"
          + " <code>grid.bottom_right</code> define the bounding box in latitude/longitude, placing"
          + " our forest in the Sierra Nevada. <code>grid.patch</code> names the patch type that"
          + " fills every cell in the grid.",
      },
      {
        id: "buildup-3-initialization",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "  ForeverTree.init =",
          "    create 10 count of ForeverTree",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "end organism",
        ],
        heading: "Initialization",
        body: "Patches come to life through initialization. The <code>ForeverTree.init</code>"
          + " attribute is evaluated once at the start of the simulation; here it creates 10"
          + " ForeverTree organisms in every patch. Josh handles the bookkeeping — each created"
          + " organism is tracked and updated independently across every time step.",
      },
      {
        id: "buildup-4-external-data",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "  ForeverTree.init =",
          "    create 10 count of ForeverTree",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "  clampedTemp.step =",
          "    limit (external temperature)",
          "    to [270 K, 330 K]",
          "",
          "  temperatureImpact.step =",
          "    map clampedTemp",
          "    from [270 K, 330 K]",
          "    to [0%, 100%] quadratic(true)",
          "",
          "  precipImpact.step =",
          "    map (external precipitation as mm)",
          "    from [300 mm, 500 mm]",
          "    to [meta.minPrecipImpactPct, 100%] sigmoid",
          "",
          "end organism",
        ],
        heading: "External Data",
        body: "Real simulations respond to real-world conditions. Josh can read geospatial data"
          + " files aligned to the simulation grid. Here we pull in gridded temperature and"
          + " precipitation rasters. <code>clampedTemp</code> bounds the temperature to a"
          + " physiologically meaningful range. <code>temperatureImpact</code> maps that clamped"
          + " value to a growth modifier using a quadratic curve, while <code>precipImpact</code>"
          + " uses a sigmoid to translate precipitation into a fractional growth effect.",
      },
      {
        id: "buildup-5-forevertree",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "  ForeverTree.init =",
          "    create 10 count of ForeverTree",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "  age.init = 0 year",
          "  age.step = prior.age + 1 year",
          "  height.init = 0 m",
          "",
          "  clampedTemp.step =",
          "    limit (external temperature)",
          "    to [270 K, 330 K]",
          "",
          "  temperatureImpact.step =",
          "    map clampedTemp",
          "    from [270 K, 330 K]",
          "    to [0%, 100%] quadratic(true)",
          "",
          "  precipImpact.step =",
          "    map (external precipitation as mm)",
          "    from [300 mm, 500 mm]",
          "    to [meta.minPrecipImpactPct, 100%] sigmoid",
          "",
          "  stochastic.step =",
          "    sample normal",
          "    with mean of 100% std of 5%",
          "",
          "  newGrowth.step =",
          "    meta.maxNewGrowth * temperatureImpact * precipImpact * stochastic",
          "",
          "  height.step = prior.height + newGrowth",
          "",
          "end organism",
        ],
        heading: "ForeverTree",
        body: "With climate drivers in place, we can define how a ForeverTree actually grows."
          + " Each organism tracks its <code>age</code> and <code>height</code> across time steps."
          + " A <code>stochastic</code> term samples a normal distribution each step, introducing"
          + " natural variability. <code>newGrowth</code> multiplies the maximum possible growth"
          + " rate by the temperature impact, precipitation impact, and stochastic factors."
          + " Finally, <code>height.step</code> accumulates growth from the prior step onward.",
      },
      {
        id: "buildup-6-units",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "  ForeverTree.init =",
          "    create 10 count of ForeverTree",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "  age.init = 0 year",
          "  age.step = prior.age + 1 year",
          "  height.init = 0 m",
          "",
          "  clampedTemp.step =",
          "    limit (external temperature)",
          "    to [270 K, 330 K]",
          "",
          "  temperatureImpact.step =",
          "    map clampedTemp",
          "    from [270 K, 330 K]",
          "    to [0%, 100%] quadratic(true)",
          "",
          "  precipImpact.step =",
          "    map (external precipitation as mm)",
          "    from [300 mm, 500 mm]",
          "    to [meta.minPrecipImpactPct, 100%] sigmoid",
          "",
          "  stochastic.step =",
          "    sample normal",
          "    with mean of 100% std of 5%",
          "",
          "  newGrowth.step =",
          "    meta.maxNewGrowth * temperatureImpact * precipImpact * stochastic",
          "",
          "  height.step = prior.height + newGrowth",
          "",
          "end organism",
          "",
          "start unit kgm2s",
          "",
          "  mm = current * 31536000",
          "",
          "end unit",
          "",
          "start unit mm",
          "",
          "  alias millimeter",
          "  alias millimeters",
          "  m = current / 1000",
          "",
          "end unit",
          "",
          "start unit K",
          "",
          "  alias Kelvin",
          "  alias Kelvins",
          "",
          "end unit",
        ],
        heading: "Units",
        body: "Josh has a built-in unit system. Rather than tracking unit conversions by hand,"
          + " you declare conversion rules in <code>start unit</code> blocks. Here we define that"
          + " <code>kgm2s</code> (the SI unit for precipitation flux from the climate data)"
          + " converts to <code>mm</code> per year, and that <code>mm</code> converts to"
          + " <code>m</code>. Josh applies these automatically whenever a value is used in a"
          + " context requiring a different unit.",
      },
      {
        id: "buildup-7-export",
        kind: "buildup",
        codeSnapshot: [
          "start simulation Main",
          "",
          "  grid.size = 16000 m",
          "  grid.top_left =",
          "    36.73 degrees latitude, -119.52 degrees longitude",
          "  grid.bottom_right =",
          "    35.80 degrees latitude, -117.98 degrees longitude",
          "  grid.patch = \"Default\"",
          "",
          "  steps.low = 0 count",
          "  steps.high = 10 count",
          "",
          "  minPrecipImpactPct.init = config forevertree.minPrecipImpactPct",
          "  maxNewGrowth.init = config forevertree.maxNewGrowth",
          "",
          "  exportFiles.patch = \"memory://editor/patches\"",
          "",
          "end simulation",
          "",
          "start patch Default",
          "",
          "  ForeverTree.init =",
          "    create 10 count of ForeverTree",
          "",
          "  export.year.step = 2024 count + meta.stepCount",
          "  export.nTrees.step = count(ForeverTree)",
          "  export.meanHeight.step = mean(ForeverTree.height)",
          "",
          "end patch",
          "",
          "start organism ForeverTree",
          "",
          "  age.init = 0 year",
          "  age.step = prior.age + 1 year",
          "  height.init = 0 m",
          "",
          "  clampedTemp.step =",
          "    limit (external temperature)",
          "    to [270 K, 330 K]",
          "",
          "  temperatureImpact.step =",
          "    map clampedTemp",
          "    from [270 K, 330 K]",
          "    to [0%, 100%] quadratic(true)",
          "",
          "  precipImpact.step =",
          "    map (external precipitation as mm)",
          "    from [300 mm, 500 mm]",
          "    to [meta.minPrecipImpactPct, 100%] sigmoid",
          "",
          "  stochastic.step =",
          "    sample normal",
          "    with mean of 100% std of 5%",
          "",
          "  newGrowth.step =",
          "    meta.maxNewGrowth * temperatureImpact * precipImpact * stochastic",
          "",
          "  height.step = prior.height + newGrowth",
          "",
          "end organism",
          "",
          "start unit kgm2s",
          "",
          "  mm = current * 31536000",
          "",
          "end unit",
          "",
          "start unit mm",
          "",
          "  alias millimeter",
          "  alias millimeters",
          "  m = current / 1000",
          "",
          "end unit",
          "",
          "start unit K",
          "",
          "  alias Kelvin",
          "  alias Kelvins",
          "",
          "end unit",
        ],
        heading: "Export & Config",
        body: "The final pieces wire the simulation to the outside world."
          + " <code>steps.low</code> and <code>steps.high</code> define the time range (here"
          + " years 0–10). The <code>config</code> keyword pulls tunable parameters from a"
          + " <code>.jshc</code> configuration file, keeping the model code clean."
          + " <code>exportFiles.patch</code> names the in-memory destination for results, and the"
          + " three <code>export.*</code> lines in the patch record the year, tree count, and mean"
          + " height at every step — the data we will visualise next.",
      },
    ];
  }

  /**
   * Transition from the step at fromIndex to the current _currentIndex.
   *
   * Cross-fades the old phase section out while fading the new one in simultaneously, using a
   * single shared TRANSITION_MS duration so Welcome→step and step→step feel uniform (~0.65s).
   * Updates aria-hidden, renders build-up content if applicable, and updates nav button state.
   *
   * @param {number|null} fromIndex - The step index being transitioned away from, or null on init.
   */
  _render(fromIndex) {
    const self = this;
    const TRANSITION_MS = 650;

    const toStep = self._steps[self._currentIndex];
    const fromStep = fromIndex !== null ? self._steps[fromIndex] : null;

    const toSection = self._getSectionForStep(toStep);
    const fromSection = fromStep ? self._getSectionForStep(fromStep) : null;

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const isCrossPhase = fromSection && fromSection !== toSection;

    if (toStep.kind === "buildup") {
      const prevStep = fromStep && fromStep.kind === "buildup" ? fromStep : null;
      self._renderBuildup(toStep, prevStep, fromIndex !== null && fromIndex > self._currentIndex);
    }

    if (isCrossPhase) {
      if (prefersReducedMotion) {
        fromSection.classList.remove("active");
        fromSection.setAttribute("aria-hidden", "true");
        toSection.classList.add("active");
        toSection.setAttribute("aria-hidden", "false");
      } else {
        fromSection.classList.add("fade-out");
        toSection.classList.add("active");
        toSection.setAttribute("aria-hidden", "false");
        toSection.classList.add("fade-in");
        setTimeout(() => {
          fromSection.classList.remove("active");
          fromSection.classList.remove("fade-out");
          fromSection.setAttribute("aria-hidden", "true");
          toSection.classList.remove("fade-in");
        }, TRANSITION_MS);
      }
    } else {
      toSection.classList.add("active");
      toSection.setAttribute("aria-hidden", "false");
    }

    self._updateNavButtons();

    setTimeout(() => {
      const focusTarget = toSection.querySelector("button, [tabindex]") || toSection;
      if (focusTarget && typeof focusTarget.focus === "function") {
        focusTarget.focus();
      }
    }, isCrossPhase ? TRANSITION_MS : 20);
  }

  /**
   * Compute an LCS-based diff between two line arrays.
   *
   * Returns a Set of indices into `toLines` that are "added" — i.e. not part of the longest
   * common subsequence with `prevLines`. Lines present in both arrays at any position (not just
   * the same index) are considered unchanged; only genuinely new lines receive .code-line-added.
   *
   * Uses classic DP table construction followed by backtracking. Performance is O(m*n) which is
   * acceptable for the small snapshots used here (≤ 81 lines).
   *
   * Blank lines and duplicate content lines are handled correctly by the LCS algorithm — a blank
   * line shared between both snapshots counts as a common element at that position in the LCS.
   *
   * @param {string[]} prevLines - The previous snapshot lines.
   * @param {string[]} toLines - The new snapshot lines.
   * @returns {Set<number>} Set of indices in `toLines` that are additions (not in LCS).
   */
  _lcsAddedIndices(prevLines, toLines) {
    const m = prevLines.length;
    const n = toLines.length;

    // Build DP table: dp[i][j] = LCS length of prevLines[0..i-1] and toLines[0..j-1].
    const dp = [];
    for (let i = 0; i <= m; i++) {
      dp.push(new Array(n + 1).fill(0));
    }
    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (prevLines[i - 1] === toLines[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }

    // Backtrack to find which indices in toLines are part of the LCS (i.e. unchanged).
    const inLcs = new Set();
    let i = m;
    let j = n;
    while (i > 0 && j > 0) {
      if (prevLines[i - 1] === toLines[j - 1]) {
        // This toLines[j-1] is part of the LCS — not an addition.
        inLcs.add(j - 1);
        i--;
        j--;
      } else if (dp[i - 1][j] >= dp[i][j - 1]) {
        i--;
      } else {
        j--;
      }
    }

    // Every toLines index NOT in the LCS is an added line.
    const added = new Set();
    for (let k = 0; k < n; k++) {
      if (!inLcs.has(k)) {
        added.add(k);
      }
    }
    return added;
  }

  /**
   * Render the build-up panel for the given step descriptor.
   *
   * Computes an LCS-based diff against the previous substep's snapshot and applies .code-line-added
   * only to lines that are genuinely new additions (forward navigation only). Each line is rendered
   * as a block <div class="code-line"> so lines stack vertically without explicit newline nodes.
   * Syntax highlighting is applied per-line via Prism if available, with graceful plain-text
   * fallback when Prism is not loaded (e.g. opening index.html without running install_deps.sh).
   * Going backward renders the earlier snapshot without any added-line animation.
   *
   * @param {Object} toStep - The step descriptor being rendered.
   * @param {Object|null} prevStep - The previous build-up step, or null if none.
   * @param {boolean} goingBack - True when navigating backward.
   */
  _renderBuildup(toStep, prevStep, goingBack) {
    const self = this;
    const codeDisplay = document.getElementById("buildup-code-display");
    const textPanel = document.getElementById("buildup-text-panel");

    if (!codeDisplay || !textPanel) {
      return;
    }

    const prevSnapshot = prevStep ? prevStep.codeSnapshot : [];
    const toSnapshot = toStep.codeSnapshot;

    // Compute which lines in the new snapshot are genuine additions (LCS-based diff).
    // Going backward never animates — treat all lines as unchanged.
    const addedIndices = goingBack
      ? new Set()
      : self._lcsAddedIndices(prevSnapshot, toSnapshot);

    const useHighlight = typeof window !== "undefined"
      && window.Prism
      && Prism.languages
      && Prism.languages.joshlang;

    codeDisplay.innerHTML = "";

    toSnapshot.forEach((line, i) => {
      const div = document.createElement("div");
      div.className = "code-line";

      if (line === "") {
        div.innerHTML = "&#8203;";
      } else if (useHighlight) {
        div.innerHTML = Prism.highlight(line, Prism.languages.joshlang, "joshlang");
      } else {
        div.textContent = line;
      }

      if (addedIndices.has(i)) {
        div.classList.add("code-line-added");
      }

      codeDisplay.appendChild(div);
    });

    textPanel.innerHTML = "";
    if (toStep.heading) {
      const h2 = document.createElement("h2");
      h2.textContent = toStep.heading;
      textPanel.appendChild(h2);
    }
    if (toStep.body) {
      const p = document.createElement("p");
      p.innerHTML = toStep.body;
      textPanel.appendChild(p);
    }
  }

  /**
   * Show or hide the prev/next buttons based on the current step index and step kind.
   *
   * The welcome step has no prev/next buttons of its own (it has its own next button wired
   * separately). For build-up steps the nav buttons inside #phase-buildup are managed here.
   */
  _updateNavButtons() {
    const self = this;
    const step = self._steps[self._currentIndex];

    const buildupPrev = document.getElementById("buildup-prev-button");
    const buildupNext = document.getElementById("buildup-next-button");

    if (!buildupPrev || !buildupNext) {
      return;
    }

    if (step.kind === "buildup") {
      buildupPrev.disabled = false;
      buildupNext.disabled = self._currentIndex >= self._steps.length - 1;
    } else {
      buildupPrev.disabled = true;
      buildupNext.disabled = false;
    }
  }

  /**
   * Return the section DOM element for a given step descriptor.
   *
   * @param {Object} step - A step descriptor.
   * @returns {HTMLElement} The corresponding section element.
   */
  _getSectionForStep(step) {
    const kindToId = {
      welcome: "phase-welcome",
      buildup: "phase-buildup",
      playground: "phase-playground",
      conclusion: "phase-conclusion",
    };
    const id = kindToId[step.kind] || "phase-welcome";
    return document.getElementById(id);
  }
}


export {NarrativePresenter};
