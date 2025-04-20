/**
 * Structures describing geometric intersection detection algorithms.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;


/**
 * Utility class for detecting intersections between different geometric shapes.
 */
public class IntersectionDetector {

  /**
   * Determines if two grid shapes intersect.
   *
   * <p>This method serves as the main entry point for intersection detection, routing the
   * calculation to specialized methods based on the geometric types involved.</p>
   *
   * @param shape1 The first shape to check for intersection
   * @param shape2 The second shape to check for intersection
   * @return true if the shapes intersect, false otherwise
   */
  public static boolean intersect(GridShape shape1, GridShape shape2) {
    return switch (shape1.getGridShapeType()) {
      case POINT -> switch (shape2.getGridShapeType()) {
        case POINT -> pointPointIntersection(shape1, shape2);
        case SQUARE -> squareSquareIntersection(shape1, shape2);
        case CIRCLE -> circleCircleIntersection(shape1, shape2);
      };
      case SQUARE -> switch (shape2.getGridShapeType()) {
        case POINT -> squareSquareIntersection(shape1, shape2);
        case SQUARE -> squareSquareIntersection(shape1, shape2);
        case CIRCLE -> squareCircleIntersection(shape1, shape2);
      };
      case CIRCLE -> switch (shape2.getGridShapeType()) {
        case POINT -> shape1.equals(shape2);
        case SQUARE -> squareCircleIntersection(shape2, shape1);
        case CIRCLE -> circleCircleIntersection(shape1, shape2);
      };
    };
  }

  /**
   * Determines if two points intersect (are at the same location).
   *
   * @param point1 The first point to check
   * @param point2 The second point to check
   * @return true if the points are at the same location, false otherwise
   */
  private static boolean pointPointIntersection(GridShape point1, GridShape point2) {
    return point1.equals(point2);
  }

  /**
   * Determines if two squares intersect by checking for overlap in their ranges.
   *
   * @param square1 The first square to check
   * @param square2 The second square to check
   * @return true if the squares overlap, false otherwise
   */
  private static boolean squareSquareIntersection(GridShape square1, GridShape square2) {
    BigDecimal width1 = square1.getWidth();
    BigDecimal radius1 = width1.divide(BigDecimal.TWO);
    BigDecimal x1Min = square1.getCenterX().subtract(radius1);
    BigDecimal x1Max = square1.getCenterX().add(radius1);
    BigDecimal y1Min = square1.getCenterY().subtract(radius1);
    BigDecimal y1Max = square1.getCenterY().add(radius1);

    BigDecimal width2 = square2.getWidth();
    BigDecimal radius2 = width2.divide(BigDecimal.TWO);
    BigDecimal x2Min = square2.getCenterX().subtract(radius2);
    BigDecimal x2Max = square2.getCenterX().add(radius2);
    BigDecimal y2Min = square2.getCenterY().subtract(radius2);
    BigDecimal y2Max = square2.getCenterY().add(radius2);

    boolean inRangeX = x1Min.compareTo(x2Max) < 0 && x1Max.compareTo(x2Min) > 0;
    boolean inRangeY = y1Min.compareTo(y2Max) < 0 && y1Max.compareTo(y2Min) > 0;

    return inRangeX && inRangeY;
  }

  /**
   * Determines if two circles intersect by comparing the distance between centers.
   *
   * @param circle1 The first circle to check
   * @param circle2 The second circle to check
   * @return true if the circles overlap, false otherwise
   */
  private static boolean circleCircleIntersection(GridShape circle1, GridShape circle2) {
    BigDecimal distanceForX = circle1.getCenterX().subtract(circle2.getCenterX());
    BigDecimal distanceSquaredForX = distanceForX.multiply(distanceForX);
    BigDecimal distanceForY = circle1.getCenterY().subtract(circle2.getCenterY());
    BigDecimal distanceSquaredForY = distanceForY.multiply(distanceForY);
    BigDecimal distanceSquared = distanceSquaredForX.add(distanceSquaredForY);

    BigDecimal radius1 = circle1.getWidth().divide(BigDecimal.TWO);
    BigDecimal radius2 = circle2.getWidth().divide(BigDecimal.TWO);
    BigDecimal reach = radius1.add(radius2);
    BigDecimal reachSquared = reach.multiply(reach);

    return distanceSquared.compareTo(reachSquared) < 0;
  }

  /**
   * Determines if a square and circle intersect.
   *
   * @param square The square to check
   * @param circle The circle to check
   * @return true if the square and circle overlap, false otherwise
   */
  private static boolean squareCircleIntersection(GridShape square, GridShape circle) {
    BigDecimal squareRadius = square.getWidth().divide(BigDecimal.TWO);
    BigDecimal circleRadius = circle.getWidth().divide(BigDecimal.TWO);

    BigDecimal squareMinX = square.getCenterX().subtract(squareRadius);
    BigDecimal squareMaxX = square.getCenterX().add(squareRadius);
    BigDecimal squareMinY = square.getCenterY().subtract(squareRadius);
    BigDecimal squareMaxY = square.getCenterY().add(squareRadius);

    BigDecimal circleCenterX = circle.getCenterX();
    BigDecimal circleCenterY = circle.getCenterY();

    BigDecimal closestX = circleCenterX.max(squareMinX).min(squareMaxX);
    BigDecimal closestY = circleCenterY.max(squareMinY).min(squareMaxY);

    BigDecimal distanceX = circleCenterX.subtract(closestX);
    BigDecimal distanceY = circleCenterY.subtract(closestY);

    BigDecimal distanceSquaredX = distanceX.multiply(distanceX);
    BigDecimal distanceSquaredY = distanceY.multiply(distanceY);
    BigDecimal distanceSquared = distanceSquaredX.add(distanceSquaredY);
    BigDecimal circleRadiusSquared = circleRadius.multiply(circleRadius);

    return distanceSquared.compareTo(circleRadiusSquared) < 0;
  }

}
