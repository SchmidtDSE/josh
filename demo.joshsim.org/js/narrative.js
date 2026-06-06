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
 * Full accumulated code snapshot for the forevertree_wasm.josh model.
 *
 * Defined once here and referenced by both the substep-7 descriptor and the PlaygroundPresenter
 * so the model text has a single source of truth within this demo.
 */
const FOREVERTREE_WASM_SNAPSHOT = [
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
];


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
   * Provide the PlaygroundPresenter instance so NarrativePresenter can call onShow().
   *
   * Must be called after construction but before the user navigates to the playground step.
   *
   * @param {PlaygroundPresenter} presenter - The playground presenter instance.
   */
  setPlaygroundPresenter(presenter) {
    this._playgroundPresenter = presenter;
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

    const playgroundPrev = document.getElementById("playground-prev-button");
    if (playgroundPrev) {
      playgroundPrev.addEventListener("click", () => self.goPrev());
    }

    const playgroundNext = document.getElementById("playground-next-button");
    if (playgroundNext) {
      playgroundNext.addEventListener("click", () => self.goNext());
    }

    const conclusionPrev = document.getElementById("conclusion-prev-button");
    if (conclusionPrev) {
      conclusionPrev.addEventListener("click", () => self.goPrev());
    }

    self._buildStepper();
    self._buildTableOfContents();
    self._render(null);
  }

  /**
   * Build the stepper nav DOM once, wiring click handlers to goTo(globalIndex).
   *
   * Derives step titles from the heading fields of all buildup-kind steps in _steps.
   * The stepper is a <nav> containing a "Step N of M" label followed by one <button>
   * per build-up substep. Click handlers are attached once here; state (active/completed/
   * upcoming classes and aria-current) is updated by _updateStepper() on each render.
   */
  _buildStepper() {
    const self = this;
    const nav = document.getElementById("buildup-stepper");
    if (!nav) {
      return;
    }

    // Collect build-up steps and their global indices.
    self._stepperItems = [];
    self._steps.forEach((step, globalIndex) => {
      if (step.kind === "buildup") {
        self._stepperItems.push({ globalIndex, heading: step.heading });
      }
    });

    // One button per build-up substep.
    const list = document.createElement("ol");
    list.className = "stepper-list";
    self._stepperItems.forEach((item, localIdx) => {
      const li = document.createElement("li");
      li.className = "stepper-item";

      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "stepper-btn";
      btn.textContent = item.heading;
      btn.dataset.globalIndex = String(item.globalIndex);
      btn.addEventListener("click", () => self.goTo(item.globalIndex));

      li.appendChild(btn);
      list.appendChild(li);
      item.button = btn;
      item.li = li;
    });
    nav.appendChild(list);
  }

  /**
   * Update stepper visual state to reflect the current step.
   *
   * Called from _render after every navigation. Sets completed/current/upcoming classes
   * and aria-current on each stepper pill.
   */
  _updateStepper() {
    const self = this;
    if (!self._stepperItems || self._stepperItems.length === 0) {
      return;
    }

    const currentStep = self._steps[self._currentIndex];
    const isBuildup = currentStep && currentStep.kind === "buildup";

    // Find local index of current step in stepper (or -1 if not a buildup step).
    let currentLocal = -1;
    if (isBuildup) {
      self._stepperItems.forEach((item, localIdx) => {
        if (item.globalIndex === self._currentIndex) {
          currentLocal = localIdx;
        }
      });
    }

    // Update each button's state class and aria attributes.
    self._stepperItems.forEach((item, localIdx) => {
      const btn = item.button;
      if (!btn) {
        return;
      }
      btn.classList.remove("stepper-btn--current", "stepper-btn--completed", "stepper-btn--upcoming");
      btn.removeAttribute("aria-current");

      if (currentLocal < 0) {
        btn.classList.add("stepper-btn--upcoming");
      } else if (localIdx < currentLocal) {
        btn.classList.add("stepper-btn--completed");
      } else if (localIdx === currentLocal) {
        btn.classList.add("stepper-btn--current");
        btn.setAttribute("aria-current", "step");
      } else {
        btn.classList.add("stepper-btn--upcoming");
      }
    });
  }

  /**
   * Derive a human-readable label for a step for use in the table of contents.
   *
   * Build-up steps reuse their heading field (the canonical name authored in _buildSteps).
   * Non-buildup steps fall back to a small kind-to-label map.
   *
   * @param {Object} step - A step descriptor from _steps.
   * @returns {string} The ToC label for this step.
   */
  _getTocLabel(step) {
    if (step.heading) {
      return step.heading;
    }
    const kindLabels = {
      welcome: "Welcome",
      playground: "Try it yourself",
      conclusion: "Next steps",
    };
    return kindLabels[step.kind] || step.id;
  }

  /**
   * Build the table of contents dialog DOM once, wiring click handlers and open/close controls.
   *
   * Called once from _setup() after _buildStepper(). Populates #toc-list with one <li>/<button>
   * per step. Stores button references in self._tocItems for use by _updateTableOfContents().
   * Wires #toc-open-button to dialog.showModal() and #toc-close-button to dialog.close().
   * Native <dialog> provides focus-trapping, Esc-to-close, and focus-return-to-trigger for free.
   */
  _buildTableOfContents() {
    const self = this;

    const openButton = document.getElementById("toc-open-button");
    const dialog = document.getElementById("toc-dialog");
    const list = document.getElementById("toc-list");
    const closeButton = document.getElementById("toc-close-button");

    if (!openButton || !dialog || !list || !closeButton) {
      return;
    }

    openButton.addEventListener("click", () => dialog.showModal());
    closeButton.addEventListener("click", () => dialog.close());

    self._tocItems = [];

    self._steps.forEach((step, index) => {
      const li = document.createElement("li");

      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "toc-item-btn";
      btn.textContent = self._getTocLabel(step);
      btn.addEventListener("click", () => {
        self.goTo(index);
        dialog.close();
      });

      li.appendChild(btn);
      list.appendChild(li);

      self._tocItems.push({ button: btn, index });
    });
  }

  /**
   * Update table of contents visual state to reflect the current step.
   *
   * Called from _render() after every navigation. Sets aria-current="step" on the active
   * step's button and removes it from all other buttons.
   */
  _updateTableOfContents() {
    const self = this;
    if (!self._tocItems || self._tocItems.length === 0) {
      return;
    }

    self._tocItems.forEach((item) => {
      if (item.index === self._currentIndex) {
        item.button.setAttribute("aria-current", "step");
      } else {
        item.button.removeAttribute("aria-current");
      }
    });
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
        lede: "Every Josh model is built from a few labeled blocks. Before adding behavior, we sketch the empty shell.",
        body: [
          "A Josh model has three kinds of entities: a <strong>simulation</strong> (global settings and"
            + " the grid), a <strong>patch</strong> (a location on that grid), and an"
            + " <strong>organism</strong> (something that lives there — here, a ForeverTree). We start"
            + " with three empty blocks and fill them in over the next steps.",
          "More precisely: <strong>simulation</strong> defines the spatial extent, temporal range,"
            + " export targets, and landscape-level logic; <strong>patch</strong> describes a grid"
            + " cell, an initialization of organisms, per-step computations, and exported summary"
            + " statistics; and <strong>organism</strong> defines the attributes and per-step behavior"
            + " of an individual organism (“agent”), optionally including state transitions."
        ],
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
        lede: "First, tell Josh where in the world the model lives — and how finely to slice it up.",
        body: [
          "Every Josh simulation runs on a spatial grid. <code>grid.size</code> sets the resolution of"
            + " each patch (here 16 km × 16 km cells), <code>grid.top_left</code> and"
            + " <code>grid.bottom_right</code> place the bounding box on a real map — our forest sits"
            + " in the Sierra Nevada — and <code>grid.patch</code> names the patch type that fills"
            + " every cell.",
          "Concretely, this lays out 16 km square patches over a fixed lat/lon bounding box (36.73,"
            + " -119.52 to 35.80, -117.98 degrees)."
        ],
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
        lede: "Now populate the landscape — every patch starts with a small stand of trees.",
        body: [
          "Patches come to life through initialization. <code>ForeverTree.init</code> is evaluated once"
            + " at the start and creates 10 ForeverTree organisms in every patch; Josh then tracks and"
            + " updates each one independently across every time step.",
          "In other words, there are ten ForeverTree agents per patch at t0 (no mortality, so the"
            + " count stays constant)."
        ],
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
        lede: "Real forests respond to real weather — so we feed in actual temperature and rainfall maps.",
        body: [
          "Josh reads gridded climate rasters aligned to the model. <code>clampedTemp</code> keeps"
            + " temperature within a survivable range, <code>temperatureImpact</code> turns that into"
            + " a growth multiplier with a quadratic curve, and <code>precipImpact</code> does the"
            + " same for rainfall with a smooth (sigmoid) curve.",
          "Under the hood, climate rasters (geotiff/COG, NetCDF) are preprocessed once into"
            + " grid-aligned <code>.jshd</code> layers, so at runtime <code>external</code> is a plain"
            + " per-patch read; the user never writes alignment or interpolation logic."
        ],
        figuresHtml: "<div class=\"ext-figures\">"
          + "<figure><img src=\"img/eco_temp_spatial.png\""
          + " alt=\"Map of input temperature in Kelvin across the simulation grid, warmer to the south\"></figure>"
          + "<figure><img src=\"img/eco_temp_domain.png\""
          + " alt=\"Temperature growth response: a quadratic curve peaking near 300 K and zero outside 270 to 330 K\"></figure>"
          + "<figure><img src=\"img/eco_precip_spatial.png\""
          + " alt=\"Map of input precipitation in millimeters per year across the grid, wetter to the west\"></figure>"
          + "<figure><img src=\"img/eco_precip_domain.png\""
          + " alt=\"Precipitation growth response: a sigmoid rising with precipitation between 300 and 500 mm\"></figure>"
          + "</div>"
          + "<p class=\"ext-figures-caption\">The maps show the gridded climate Josh reads in;"
          + " the curves show how each value becomes a growth multiplier — so you can see why"
          + " mid-range temperatures and wetter patches drive more growth.</p>",
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
        lede: "With the climate drivers in place, define how a ForeverTree actually grows each year.",
        body: [
          "Each organism tracks its <code>age</code> and <code>height</code> over time. A"
            + " <code>stochastic</code> term samples a normal distribution each step for natural"
            + " variability, <code>newGrowth</code> multiplies the maximum growth rate by the"
            + " temperature, precipitation, and stochastic factors, and <code>height.step</code>"
            + " accumulates that growth from the prior step onward.",
          "So the annual increment combines three unitless factors — a unimodal temperature impact"
            + " that peaks mid-window and is zero at the edges, a saturating precipitation impact, and"
            + " a multiplicative noise term — and growth is choked off whenever either climate driver"
            + " is unfavorable."
        ],
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
        lede: "Mixing meters, millimeters, and Kelvin? Josh handles the unit conversions for you.",
        body: [
          "Rather than tracking conversions by hand, you declare rules in <code>start unit</code>"
            + " blocks. Here <code>kgm2s</code> (the climate data's precipitation-flux unit) converts"
            + " to <code>mm</code> per year, and <code>mm</code> converts to <code>m</code>; Josh"
            + " applies these automatically whenever a value is used where a different unit is"
            + " expected.",
          "This is built-in support for automatic unit conversions between compatible types (like"
            + " Fahrenheit to Celsius). The <code>as mm</code> cast invokes the registered"
            + " conversion — kg m⁻²s⁻¹ × 31536000 → mm yr⁻¹ — applied automatically wherever a target"
            + " unit is named."
        ],
      },
      {
        id: "buildup-7-export",
        kind: "buildup",
        codeSnapshot: FOREVERTREE_WASM_SNAPSHOT,
        heading: "Export & Config",
        lede: "Finally, choose how long to run, expose the knobs, and say what to record.",
        body: [
          "<code>steps.low</code> and <code>steps.high</code> set the time range (years 0–10),"
            + " <code>exportFiles.patch</code> names where results go, and the three"
            + " <code>export.*</code> lines record the year, tree count, and mean height at every"
            + " step — the data we visualize next. Stochastic replicates share the same climate"
            + " forcing and initial conditions, writing one row per (patch, year, replicate).",
          "The <code>config</code> keyword keeps the tunable knobs out of the model and in a"
            + " companion <code>.jshc</code> file, so collaborators can re-run with new values"
            + " without touching the code. Here it supplies <code>minPrecipImpactPct</code> and"
            + " <code>maxNewGrowth</code>:"
        ],
        figuresHtml: "<div class=\"config-example\">"
          + "<div class=\"config-example-label\">forevertree.jshc</div>"
          + "<pre><code class=\"language-joshlang\"># How much can a ForeverTree grow\n"
          + "# in one year?\n"
          + "maxNewGrowth = 1 m\n"
          + "\n"
          + "# How much is growth rate reduced\n"
          + "# by drought in a given year?\n"
          + "#  (at 0%, drought halts growth)\n"
          + "#  (at 100%, no effect on growth)\n"
          + "minPrecipImpactPct = 0 %</code></pre>"
          + "</div>",
      },
      {
        id: "playground",
        kind: "playground",
        codeSnapshot: [],
        heading: null,
        body: null,
      },
      {
        id: "conclusion",
        kind: "conclusion",
        codeSnapshot: [],
        heading: null,
        body: null,
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
        if (toStep.kind === "playground" && self._playgroundPresenter) {
          self._playgroundPresenter.onShow();
        }
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
          if (toStep.kind === "playground" && self._playgroundPresenter) {
            self._playgroundPresenter.onShow();
          }
        }, TRANSITION_MS);
      }
    } else {
      toSection.classList.add("active");
      toSection.setAttribute("aria-hidden", "false");
    }

    self._updateNavButtons();
    self._updateStepper();
    self._updateTableOfContents();

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
    // Going backward never animates. The initial reveal (no previous build-up step,
    // e.g. arriving from Welcome) also does not glow — there is no prior state to
    // diff against, so the whole skeleton would otherwise flash green and settle.
    // Only incremental additions between build-up substeps glow.
    const addedIndices = (goingBack || !prevStep)
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
    // Plain-language "what is this / why care" lede, rendered larger than the body.
    if (toStep.lede) {
      const lede = document.createElement("p");
      lede.className = "buildup-lede";
      lede.innerHTML = toStep.lede;
      textPanel.appendChild(lede);
    }
    // Body is an array of paragraph HTML strings (each its own <p>); a plain string
    // is also accepted for backwards compatibility.
    if (toStep.body) {
      const paragraphs = Array.isArray(toStep.body) ? toStep.body : [toStep.body];
      paragraphs.forEach((para) => {
        const p = document.createElement("p");
        p.innerHTML = para;
        textPanel.appendChild(p);
      });
    }
    // Optional raw-HTML block (e.g. the External-Data figure grid or a config example)
    // appended after the prose. Any `language-joshlang` code inside is Prism-highlighted.
    if (toStep.figuresHtml) {
      const wrapper = document.createElement("div");
      wrapper.innerHTML = toStep.figuresHtml;
      textPanel.appendChild(wrapper);
      if (typeof window !== "undefined" && window.Prism && Prism.highlightAllUnder) {
        Prism.highlightAllUnder(wrapper);
      }
    }
  }

  /**
   * Show or hide the prev/next buttons based on the current step index and step kind.
   *
   * The welcome step has no prev/next buttons of its own (it has its own next button wired
   * separately). For build-up steps the nav buttons inside #phase-buildup are managed here.
   * For the playground step the nav buttons inside #phase-playground are managed here.
   */
  _updateNavButtons() {
    const self = this;
    const step = self._steps[self._currentIndex];

    const buildupPrev = document.getElementById("buildup-prev-button");
    const buildupNext = document.getElementById("buildup-next-button");

    if (buildupPrev && buildupNext) {
      if (step.kind === "buildup") {
        buildupPrev.disabled = false;
        buildupNext.disabled = self._currentIndex >= self._steps.length - 1;
      } else {
        buildupPrev.disabled = true;
        buildupNext.disabled = false;
      }
    }

    const playgroundPrev = document.getElementById("playground-prev-button");
    const playgroundNext = document.getElementById("playground-next-button");

    if (playgroundPrev && playgroundNext) {
      if (step.kind === "playground") {
        playgroundPrev.disabled = self._currentIndex <= 0;
        playgroundNext.disabled = self._currentIndex >= self._steps.length - 1;
      } else {
        playgroundPrev.disabled = true;
        playgroundNext.disabled = true;
      }
    }

    const conclusionPrev = document.getElementById("conclusion-prev-button");
    if (conclusionPrev) {
      conclusionPrev.disabled = step.kind !== "conclusion";
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


export {NarrativePresenter, FOREVERTREE_WASM_SNAPSHOT};
