/**
 * General supporting utilities.
 *
 * @license BSD-3-Clause
 */

const EARTH_RADIUS_METERS = 6371000;


/**
 * Calculates the distance in meters between two geographical coordinates using Haversine formula.
 *
 * @param {number} startLongitude - The longitude of the start point.
 * @param {number} startLatitude - The latitude of the start point.
 * @param {number} endLongitude - The longitude of the end point.
 * @param {number} endLatitude - The latitude of the end point.
 * @returns {number} The distance between the start and end coordinates, in meters.
 */
getDistanceMeters(startLongitude, startLatitude, endLongitude, endLatitude) {
  const angleLatitudeStart = startLatitude * Math.PI / 180;
  const angleLatitudeEnd = endLatitude * Math.PI / 180;
  const deltaLatitude = (endLatitude - startLatitude) * Math.PI / 180;
  const deltaLongitude = (endLongitude - startLongitude) * Math.PI / 180;

  const a = (
    Math.sin(deltaLatitude/2) * Math.sin(deltaLatitude/2) +
    Math.cos(angleLatitudeStart) * Math.cos(angleLatitudeEnd) *
    Math.sin(deltaLongitude/2) * Math.sin(deltaLongitude/2)
  );
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

  return EARTH_RADIUS * c;
}


export {getDistanceMeters};
