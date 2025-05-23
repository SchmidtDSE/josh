/**
 * Logic to generate summary metrics from simulation data.
 *
 * @license BSD-3-Clause
 */

import {SummarizedResult} from "model";


/**
 * Strategies for evaluating conditional statements within queries.
 * 
 * Set of conditional strateiges where x is the number to be checked, targetA is the first target
 * from the query being fulfilled, and targetB is the second target from the query being fulfilled.
 * The targetA will always be specified and acts as the threshold for exceeds and falls below.The
 * targetB will only be specified for is between. For is between, targetA is the minimum and targetB
 * is the maximum.
 */
const CONDITIONALS = {
  "exceeds": (x, targetA, targetB) => x > targetA,
  "falls below": (x, targetA, targetB) => x < targetA,
  "is between": (x, targetA, targetB) => x >= targetA && x <= targetB
}

/**
 * Strategies for generating a user-requested metric keyed by that metric name.
 *
 * Strategies for generating a user-requested metric keyed by that metric name where values are the
 * Array<number>s for which a metric is to be generated, type is the metric type like "exceeds" to
 * be used in parameterizing that metric, and targets A and B are used as inputs into that metric
 * generation if applicable.
 */
const METRIC_STRATEGIES = {
  "mean": (values, type, targetA, targetB) => math.mean(values),
  "median": (values, type, targetA, targetB) => math.median(values),
  "min": (values, type, targetA, targetB) => math.min(values),
  "max": (values, type, targetA, targetB) => math.max(values),
  "std": (values, type, targetA, targetB) => math.std(values),
  "probability": (values, type, targetA, targetB) => {
    const conditional = CONDITIONALS[type];
    const countTotal = values.length;

    const matching = values.filter((candidate) => conditional(candidate, targetA, targetB));
    const countMatching = matching.length;

    return countMatching / countTotal;
  }
};


/**
 * Record describing which variable the user wants to analyze and how.
 *
 * Record describing which variable exported from the script that the user wants to analyze and
 * indicate how those values should be reated (mean, median, etc). If the user is calculating
 * probabilities, this will also have one or two target values.
 */
class DataQuery {

  /**
   * Create a new record of a user-requested DataQuery.
   *
   * @param {string} variable The name of the variable as exported from the user's script to be
   *     analyzed.
   * @param {string} metric The kind of metric to be calculated like mean. This will be applied both
   *     at the simulation level (like mean across all patches across all timesteps) for the scrub
   *     element or similar and patch level (like mean for each patch across all timesteps).
   * @param {?string} metricType The sub-type of metric to be calculated. For example, if metric is
   *     probability, this may be exceeds or falls below. Ignored if the metric does not have sub-
   *     types and may be null in that case.
   * @param {?number} targetA The first reference value to use for probability metrics like the
   *     minimum threshold for proability of exceeds, maximum for probablity below, and minimum
   *     for probability within range. Should be null if not a probability (value ignored).
   * @param {?number} targetB The second reference value to use for probability metrics like the
   *     maximum for probability within range. Should be null if not a probability within range.
   */
  constructor(variable, metric, metricType, targetA, targetB) {
    const self = this;
    self._variable = variable;
    self._metric = metric;
    self._metricType = metricType;
    self._targetA = targetA;
    self._targetB = targetB;
  }

  /**
   * Get the variable name being analyzed.
   * 
   * @returns {string} The variable name.
   */
  getVariable() {
    const self = this;
    return self._variable;
  }

  /**
   * Get the metric type being calculated.
   * 
   * @returns {string} The metric type.
   */
  getMetric() {
    const self = this;
    return self._metric;
  }

  /**
   * Get the metric sub-type being calculated.
   * 
   * @returns {?string} The metric sub-type like "exceeds" or "falls below" or null if the given
   *     metric at getMetric does not have a sub-type.
   */
  getMetricType() {
    const self = this;
    return self._metricType;
  }


  /**
   * Get the first target value for probability metrics.
   * 
   * @returns {?number} The first target value or null.
   */
  getTargetA() {
    const self = this;
    return self._targetA;
  }

  /**
   * Get the second target value for probability metrics.
   * 
   * @returns {?number} The second target value or null.
   */
  getTargetB() {
    const self = this;
    return self._targetB;
  }

}


/**
 * Summarize datasets according to query specified by the user by operating on patch results.
 *
 * Summarize datasets according to query specified by the user using the strategies specified in
 * METRIC_STRATEGIES and, if applicable, CONDITIONALS. This will only summarize patch-level results.
 *
 * @param {Array<SimulationResult>} target - The simulation results with one element per replicate.
 * @param {DataQuery} query - Description of the query that the user is trying to execute with the
 *     information for the metric to be generated or summarized.
 * @returns {SummarizedResult} The target replicates summarized both where there is one overall
 *     value per timestep (summarized across all patches and replicates) and where there is one
 *     value per patch per timestep (summarized across all replicates per patch / timestep).
 */
function summarizeDatasets(target, query) {
  if (target.length == 0) {
    throw "Requires at least one replicate to summarize.";
  }

  const strategy = METRIC_STRATEGIES[query.getMetric()];
  if (!strategy) {
    throw `Unknown metric: ${query.getMetric()}`;
  }

  const variable = query.getVariable();
  const metricType = query.getMetricType();
  const targetA = query.getTargetA();
  const targetB = query.getTargetB();
  const curriedStrategy = (values) => strategy(values, metricType, targetA, targetB);

  const targetFlat = target.flatMap((x) => x.getPatchResults()).filter((x) => x.hasValue(variable));

  /**
   * Get a map from key to all values from records with that key.
   *
   * @param {function} keyGetter - The funciton taking an OutputDatum from which the key should be
   *     returned.
   * @returns {Map<string, Array<number>>} Mapping from key to values found for all records with the
   *     key and with the target attribute present.
   */
  const getValuesByKey = (keyGetter) => {
    const keyedValuesUnsafe = targetFlat.map((x) => {
      return {"key": keyGetter(x), "value": x.getValue(variable)};
    });

    const keyedValues = keyedValuesUnsafe.filter((x) => x["key"] !== null);

    const valuesByKey = new Map();
    keyedValues.forEach((target) => {
      const key = target["key"];
      const value = target["value"];

      if (!valuesByKey.has(key)) {
        valuesByKey.set(key, []);
      }

      valuesByKey.get(key).push(value)
    });

    return valuesByKey;
  }

  /**
   * Get a map from key to the metric value from records with that key.
   *
   * @param {function} keyGetter - The funciton taking an OutputDatum from which the key should be
   *     returned.
   * @returns {Map<string, number>} Mapping from key to metric value.
   */
  const summarizeByKey = (keyGetter) => {
    const valuesByKey = getValuesByKey(keyGetter);
    const valueByKey = new Map();

    valuesByKey.keys().forEach((key) => {
      const values = valuesByKey.get(key);
      const value = curriedStrategy(values);
      valueByKey.set(key, value);
    });

    return valueByKey;
  };

  const firstReplicate = target[0];
  return new SummarizedResult(
    firstReplicate.getMinX(),
    firstReplicate.getMinY(), 
    firstReplicate.getMaxX(),
    firstReplicate.getMaxY(),
    summarizeByKey(getTimestepKey),
    summarizeByKey(getGridKey)
  );
}


/**
 * Extracts the timestep value from a given record.
 * 
 * @param {OutputDatum} record - The output record from which the step will be retrieved.
 * @returns {?number} The step value if it exists, or null if not present.
 */
function getTimestepKey(record) {
  if (!record.hasValue("step")) {
    return null;
  }

  return record.getValue("step");
}


/**
 * Extracts a composite key from a given record based on its timestep and position.
 *
 * @param {OutputDatum} record - The output record from which the key will be constructed.
 * @returns {?string} A string key in the format "timestep,x,y" if all values exist, or null if any
 *     are missing.
 */
function getGridKey(record) {
  const hasX = record.hasValue("position.x");
  const hasY = record.hasValue("position.y");
  const hasTime = record.hasValue("step");
  const hasRequired = hasX && hasY && hasTime;
  if (!hasRequired) {
    return null;
  }

  const timestep = Math.round(record.getValue("step"));
  const x = record.getValue("position.x").toFixed(2);
  const y = record.getValue("position.y").toFixed(2);
  return `${timestep},${x},${y}`;
}


export {DataQuery, summarizeDatasets};
