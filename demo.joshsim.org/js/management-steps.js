/**
 * Step descriptors and full-model snapshot for the ForeverTree *management* extension demo
 * (demo.joshsim.org/management).
 *
 * Mirrors the shape narrative.js expects (the same {id, kind, codeSnapshot, heading} descriptors and
 * the same four "kinds"), so the shared NarrativePresenter renders this page with no engine changes.
 * The full model below is the exact text of paper/management/management_wasm.josh; the build-up
 * snapshots are assembled from shared fragments so each step is a coherent superset of the previous
 * one and the LCS diff in narrative.js green-glows only the genuinely new lines.
 *
 * @license BSD-3-Clause
 */


/* ----------------------------------------------------------------------------------------------- *
 * Shared line fragments. Each is an array of source lines (one string per line, "" for a blank).
 * Snapshots are composed by concatenating fragments in source order, so the final composition is
 * byte-for-byte equal to management_wasm.josh (verified in CI / test).
 * ----------------------------------------------------------------------------------------------- */

// Simulation stanza ------------------------------------------------------------------------------
const SIM_HEAD = [
  "start simulation Main",
  "",
  "  grid.size = 16000 m",
  "  grid.top_left =",
  "    36.73 degrees latitude,",
  "    -119.52 degrees longitude",
  "  grid.bottom_right =",
  "    35.80 degrees latitude,",
  "    -117.98 degrees longitude",
  "  grid.patch = \"Default\"",
  "",
  "  steps.low = 0 count",
  "  steps.high = 10 count",
  "",
];
const SIM_FIRE_LINE = [
  "  fire.step = meta.stepCount == config scenario.fireStep",
  "",
];
const SIM_TAIL = [
  "  exportFiles.patch = \"memory://editor/patches\"",
  "",
  "end simulation",
  "",
];
const SIM_NOFIRE = [...SIM_HEAD, ...SIM_TAIL];
const SIM_WITHFIRE = [...SIM_HEAD, ...SIM_FIRE_LINE, ...SIM_TAIL];

// Patch fragments --------------------------------------------------------------------------------
const PATCH_OPEN = ["start patch Default", ""];
const PATCH_CLOSE = ["end patch", ""];

const ONFIRE = [
  "  onFire.step = meta.fire and ((external burned at 0) > 0 count)",
  "",
];
const MGMT_TOP = [
  "  onFire.step = meta.fire and ((external burned at 0) > 0 count)",
  "  inMgmtZone.step = (external managed at 0) > 0 count",
  "  inMgmt.step = inMgmtZone and (meta.stepCount == config scenario.managementStep)",
  "",
  "  inMgmtPlanted.step = inMgmt and (config scenario.outplantCount > 0 count)",
  "  inMgmtInvasives.step = inMgmt and (config scenario.removeInvasives > 0 count)",
  "",
];

const FT_INIT_ONLY = [
  "  ForeverTree.init = create 10 count of ForeverTree",
  "",
];
const FT_INIT_PLANT = [
  "  ForeverTree.init = create 10 count of ForeverTree",
  "  ForeverTree.step:if(inMgmtPlanted) = prior.ForeverTree | (create config scenario.outplantCount of ForeverTree)",
  "",
];

const NLIVE_ALL = [
  "  nLiveTrees.init = count(ForeverTree)",
  "  nLiveTrees.step = count(ForeverTree)",
  "",
];
const NLIVE_LIVE = [
  "  nLiveTrees.init = count(ForeverTree)",
  "  nLiveTrees.step = count(ForeverTree[ForeverTree.state != \"Burned\"])",
  "",
];

const DISPERSAL_BLOCK = [
  "  # Invasive grass disperses to neighbours: each patch emits a share of its cover,",
  "  # split among its neighbours; a patch's inflow is the sum of neighbour shares,",
  "  # read on the following step (prior.coverImmigration).",
  "  coverShare.end = {",
  "    const neighbors = Default within config scenario.dispersalRadius radial at prior",
  "    const n = limit count(neighbors) - 1 count to [1 count, 100 count]",
  "    return (prior.invasiveCover * config scenario.invasiveDispersalRate) / n",
  "  }",
  "  coverImmigration.init = 0%",
  "  coverImmigration.end = {",
  "    const neighbors = Default within config scenario.dispersalRadius radial at prior",
  "    if (count(neighbors) > 1 count) {",
  "      return sum(neighbors.coverShare)",
  "    } else {",
  "      return 0%",
  "    }",
  "  }",
  "",
];

// invasiveCover.step, in three progressively-elaborated forms.
const INVASIVE_SIMPLE = [
  "  invasiveCover.init = config scenario.invasiveMinCover",
  "  invasiveCover.step = {",
  "    const openness =",
  "      map (limit prior.nLiveTrees to [0 count, config scenario.treesForFullSuppression])",
  "      from [0 count, config scenario.treesForFullSuppression]",
  "      to [100%, 0%] linear",
  "    const grown =",
  "      limit (prior.invasiveCover + openness * config scenario.invasiveBaseGrowth)",
  "      to [config scenario.invasiveMinCover, 100%]",
  "    return grown",
  "  }",
  "",
];
const INVASIVE_DISPERSAL = [
  "  invasiveCover.init = config scenario.invasiveMinCover",
  "  invasiveCover.step = {",
  "    const openness =",
  "      map (limit prior.nLiveTrees to [0 count, config scenario.treesForFullSuppression])",
  "      from [0 count, config scenario.treesForFullSuppression]",
  "      to [100%, 0%] linear",
  "    const grown =",
  "      limit (prior.invasiveCover + openness * (config scenario.invasiveBaseGrowth + prior.coverImmigration))",
  "      to [config scenario.invasiveMinCover, 100%]",
  "    return grown",
  "  }",
  "",
];
const INVASIVE_FINAL = [
  "  invasiveCover.init = config scenario.invasiveMinCover",
  "  invasiveCover.step = {",
  "    const openness =",
  "      map (limit prior.nLiveTrees to [0 count, config scenario.treesForFullSuppression])",
  "      from [0 count, config scenario.treesForFullSuppression]",
  "      to [100%, 0%] linear",
  "    const grown =",
  "      limit (prior.invasiveCover + openness * (config scenario.invasiveBaseGrowth + prior.coverImmigration))",
  "      to [config scenario.invasiveMinCover, 100%]",
  "    return config scenario.invasiveRemovalTarget if inMgmtInvasives else grown",
  "  }",
  "",
];

// Export blocks (grow as new attributes are introduced; insertions keep the diff clean).
const EXP_YEAR = "  export.year.step = 2024 count + meta.stepCount";
const EXP_NTREES = "  export.nTrees.step = count(ForeverTree)";
const EXP_NLIVE = "  export.nLiveTrees.step = nLiveTrees";
const EXP_NBURNED = "  export.nBurned.step = count(ForeverTree[ForeverTree.state == \"Burned\"])";
const EXP_NJUV = "  export.nJuvenile.step = count(ForeverTree[ForeverTree.state == \"Juvenile\"])";
const EXP_NADULT = "  export.nAdult.step = count(ForeverTree[ForeverTree.state == \"Adult\"])";
const EXP_HEIGHT = "  export.meanHeight.step = mean(ForeverTree.height)";
const EXP_COVER = "  export.invasiveCover.step = invasiveCover";

const EXPORTS_RECAP = [EXP_YEAR, EXP_NTREES, EXP_HEIGHT, ""];
const EXPORTS_INVASIVE = [EXP_YEAR, EXP_NTREES, EXP_NLIVE, EXP_HEIGHT, EXP_COVER, ""];
const EXPORTS_FIRE = [EXP_YEAR, EXP_NTREES, EXP_NLIVE, EXP_NBURNED, EXP_HEIGHT, EXP_COVER, ""];
const EXPORTS_FINAL = [
  EXP_YEAR, EXP_NTREES, EXP_NLIVE, EXP_NBURNED, EXP_NJUV, EXP_NADULT, EXP_HEIGHT, EXP_COVER, "",
];

// Organism fragments -----------------------------------------------------------------------------
const ORG_HEAD_BASE = [
  "start organism ForeverTree",
  "",
  "  age.init = 0 year",
  "  age.step = prior.age + 1 year",
  "  height.init = 0 m",
  "",
];
const ORG_HEAD_OUTPLANT = [
  "start organism ForeverTree",
  "",
  "  age.init",
  "    :if(meta.stepCount == 0 count) = 0 year",
  "    :else = config scenario.outplantAge",
  "  age.step = prior.age + 1 year",
  "",
  "  height.init",
  "    :if(meta.stepCount == 0 count) = 0 m",
  "    :else = config scenario.outplantHeight",
  "",
];
const STATE_INIT = [
  "  state.init = \"Adult\" if height >= config scenario.smallTreeHeight else \"Juvenile\"",
  "",
];
const BURNED_STEP = [
  "  # One burn draw per tree; only true inside the fire footprint on the fire step.",
  "  burned.step = here.onFire and ((sample uniform from 0% to 100%) < config scenario.burnLikelihood)",
  "",
];
const ORG_CLIMATE = [
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
  "    to [config scenario.minPrecipImpactPct, 100%] sigmoid",
  "",
  "  stochastic.step =",
  "    sample normal",
  "    with mean of 100% std of 5%",
  "",
];
const NEWGROWTH_BASE = [
  "  newGrowth.step =",
  "    config scenario.maxNewGrowth",
  "    * temperatureImpact",
  "    * precipImpact",
  "    * stochastic",
  "",
  "  height.step = prior.height + newGrowth",
  "",
  "end organism",
];
const NEWGROWTH_INHIB = [
  "  newGrowth.step =",
  "    config scenario.maxNewGrowth",
  "    * temperatureImpact",
  "    * precipImpact",
  "    * stochastic",
  "    * growthInhibition",
  "",
  "  height.step = prior.height + newGrowth",
  "",
];
const STATE_BLOCKS_NOFIRE = [
  "  # Small trees: out-competed by invasive grass, and can mature once tall enough.",
  "  start state \"Juvenile\"",
  "    growthInhibition.step = limit (100% - here.invasiveCover) to [0%, 100%]",
  "    state.step:if(prior.height >= config scenario.smallTreeHeight) = \"Adult\"",
  "  end state",
  "",
  "  # Large trees: grow unhindered by grass.",
  "  start state \"Adult\"",
  "    growthInhibition.step = 100%",
  "  end state",
  "",
  "  # Killed by fire: no further growth.",
  "  start state \"Burned\"",
  "    growthInhibition.step = 0%",
  "  end state",
];
const STATE_BLOCKS_FIRE = [
  "  # Small trees: out-competed by invasive grass, and can mature once tall enough.",
  "  start state \"Juvenile\"",
  "    growthInhibition.step = limit (100% - here.invasiveCover) to [0%, 100%]",
  "    state.step",
  "      :if(current.burned) = \"Burned\"",
  "      :elif(prior.height >= config scenario.smallTreeHeight) = \"Adult\"",
  "  end state",
  "",
  "  # Large trees: grow unhindered by grass.",
  "  start state \"Adult\"",
  "    growthInhibition.step = 100%",
  "    state.step:if(current.burned) = \"Burned\"",
  "  end state",
  "",
  "  # Killed by fire: no further growth.",
  "  start state \"Burned\"",
  "    growthInhibition.step = 0%",
  "  end state",
];
const ORG_END = ["", "end organism"];

const ORG_V1 = [...ORG_HEAD_BASE, ...ORG_CLIMATE, ...NEWGROWTH_BASE];
const ORG_V2 = [
  ...ORG_HEAD_BASE, ...STATE_INIT, ...ORG_CLIMATE, ...NEWGROWTH_INHIB, ...STATE_BLOCKS_NOFIRE, ...ORG_END,
];
const ORG_V3 = [
  ...ORG_HEAD_BASE, ...STATE_INIT, ...BURNED_STEP, ...ORG_CLIMATE, ...NEWGROWTH_INHIB,
  ...STATE_BLOCKS_FIRE, ...ORG_END,
];
const ORG_V4 = [
  ...ORG_HEAD_OUTPLANT, ...STATE_INIT, ...BURNED_STEP, ...ORG_CLIMATE, ...NEWGROWTH_INHIB,
  ...STATE_BLOCKS_FIRE, ...ORG_END,
];

// Units (invariant) ------------------------------------------------------------------------------
const UNITS = [
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

// Patch versions ---------------------------------------------------------------------------------
const PATCH_RECAP = [...PATCH_OPEN, ...FT_INIT_ONLY, ...EXPORTS_RECAP, ...PATCH_CLOSE];
const PATCH_INVASIVE = [
  ...PATCH_OPEN, ...FT_INIT_ONLY, ...NLIVE_ALL, ...INVASIVE_SIMPLE, ...EXPORTS_INVASIVE, ...PATCH_CLOSE,
];
const PATCH_LIFE = [
  ...PATCH_OPEN, ...FT_INIT_ONLY, ...NLIVE_LIVE, ...INVASIVE_SIMPLE, ...EXPORTS_INVASIVE, ...PATCH_CLOSE,
];
const PATCH_FIRE = [
  ...PATCH_OPEN, ...ONFIRE, ...FT_INIT_ONLY, ...NLIVE_LIVE, ...INVASIVE_SIMPLE, ...EXPORTS_FIRE, ...PATCH_CLOSE,
];
const PATCH_DISP = [
  ...PATCH_OPEN, ...ONFIRE, ...FT_INIT_ONLY, ...NLIVE_LIVE, ...DISPERSAL_BLOCK, ...INVASIVE_DISPERSAL,
  ...EXPORTS_FIRE, ...PATCH_CLOSE,
];
const PATCH_MGMT = [
  ...PATCH_OPEN, ...MGMT_TOP, ...FT_INIT_PLANT, ...NLIVE_LIVE, ...DISPERSAL_BLOCK, ...INVASIVE_FINAL,
  ...EXPORTS_FIRE, ...PATCH_CLOSE,
];
const PATCH_FINAL = [
  ...PATCH_OPEN, ...MGMT_TOP, ...FT_INIT_PLANT, ...NLIVE_LIVE, ...DISPERSAL_BLOCK, ...INVASIVE_FINAL,
  ...EXPORTS_FINAL, ...PATCH_CLOSE,
];

// Full per-chapter snapshots ---------------------------------------------------------------------
const SNAP_RECAP = [...SIM_NOFIRE, ...PATCH_RECAP, ...ORG_V1, ...UNITS];
const SNAP_INVASIVE = [...SIM_NOFIRE, ...PATCH_INVASIVE, ...ORG_V1, ...UNITS];
const SNAP_LIFE = [...SIM_NOFIRE, ...PATCH_LIFE, ...ORG_V2, ...UNITS];
const SNAP_FIRE = [...SIM_WITHFIRE, ...PATCH_FIRE, ...ORG_V3, ...UNITS];
const SNAP_DISP = [...SIM_WITHFIRE, ...PATCH_DISP, ...ORG_V3, ...UNITS];
const SNAP_MGMT = [...SIM_WITHFIRE, ...PATCH_MGMT, ...ORG_V4, ...UNITS];

/**
 * The complete management model — byte-for-byte equal to paper/management/management_wasm.josh.
 * Used by the final build-up step and pre-loaded into the playground editor.
 */
const MANAGEMENT_WASM_SNAPSHOT = [...SIM_WITHFIRE, ...PATCH_FINAL, ...ORG_V4, ...UNITS];


/**
 * Ordered step descriptors for the management narrative. Same shape and kinds as the forevertree
 * steps in narrative.js, so the shared NarrativePresenter drives this page unchanged.
 */
const MANAGEMENT_STEPS = [
  {id: "welcome", kind: "welcome", codeSnapshot: [], heading: null},
  {id: "recap-forevertree", kind: "buildup", codeSnapshot: SNAP_RECAP, heading: "The ForeverTree, recapped"},
  {id: "invasive-cover", kind: "buildup", codeSnapshot: SNAP_INVASIVE, heading: "Invasive grass"},
  {id: "life-stages", kind: "buildup", codeSnapshot: SNAP_LIFE, heading: "Juvenile, Adult, Burned"},
  {id: "fire", kind: "buildup", codeSnapshot: SNAP_FIRE, heading: "Fire"},
  {id: "dispersal", kind: "buildup", codeSnapshot: SNAP_DISP, heading: "Grass spreads"},
  {id: "management", kind: "buildup", codeSnapshot: SNAP_MGMT, heading: "Management interventions"},
  {id: "export-config", kind: "buildup", codeSnapshot: MANAGEMENT_WASM_SNAPSHOT, heading: "Outputs & knobs"},
  {id: "playground", kind: "playground", codeSnapshot: [], heading: null},
  {id: "conclusion", kind: "conclusion", codeSnapshot: [], heading: null},
];


export {MANAGEMENT_STEPS, MANAGEMENT_WASM_SNAPSHOT};
