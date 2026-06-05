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
 * code-panel line-by-line diff rendering with per-line fade-in, and nav button state.
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
    ];
  }

  /**
   * Transition from the step at fromIndex to the current _currentIndex.
   *
   * Fades out the old phase section, fades in the new one, updates aria-hidden, renders build-up
   * content if applicable, and updates nav button state.
   *
   * @param {number|null} fromIndex - The step index being transitioned away from, or null on init.
   */
  _render(fromIndex) {
    const self = this;
    const toStep = self._steps[self._currentIndex];
    const fromStep = fromIndex !== null ? self._steps[fromIndex] : null;

    const toSection = self._getSectionForStep(toStep);
    const fromSection = fromStep ? self._getSectionForStep(fromStep) : null;

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (fromSection && fromSection !== toSection) {
      if (prefersReducedMotion) {
        fromSection.classList.remove("active");
        fromSection.setAttribute("aria-hidden", "true");
      } else {
        fromSection.classList.add("fade-out");
        setTimeout(() => {
          fromSection.classList.remove("active");
          fromSection.classList.remove("fade-out");
          fromSection.setAttribute("aria-hidden", "true");
        }, 400);
      }
    }

    if (toStep.kind === "buildup") {
      const prevStep = fromStep && fromStep.kind === "buildup" ? fromStep : null;
      self._renderBuildup(toStep, prevStep, fromIndex !== null && fromIndex > self._currentIndex);
    }

    if (prefersReducedMotion) {
      toSection.classList.add("active");
      toSection.setAttribute("aria-hidden", "false");
    } else {
      setTimeout(() => {
        toSection.classList.add("active");
        toSection.setAttribute("aria-hidden", "false");
        toSection.classList.add("fade-in");
        setTimeout(() => {
          toSection.classList.remove("fade-in");
        }, 400);
      }, fromSection && fromSection !== toSection ? 400 : 0);
    }

    self._updateNavButtons();

    setTimeout(() => {
      const focusTarget = toSection.querySelector("button, [tabindex]") || toSection;
      if (focusTarget && typeof focusTarget.focus === "function") {
        focusTarget.focus();
      }
    }, fromSection && fromSection !== toSection ? 420 : 20);
  }

  /**
   * Render the build-up panel for the given step descriptor.
   *
   * Computes a line-by-line diff against the previous substep's snapshot and applies .fade-in only
   * to newly added or changed lines. Going backward renders the earlier snapshot without animation.
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

    codeDisplay.innerHTML = "";

    toSnapshot.forEach((line, i) => {
      const span = document.createElement("span");
      span.className = "code-line";
      span.textContent = line;

      const isNew = goingBack
        ? false
        : (i >= prevSnapshot.length || prevSnapshot[i] !== line);

      if (isNew) {
        span.classList.add("fade-in");
      }

      codeDisplay.appendChild(span);
      codeDisplay.appendChild(document.createTextNode("\n"));
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
