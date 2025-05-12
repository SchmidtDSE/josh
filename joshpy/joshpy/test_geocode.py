import unittest

import geocode


class TestGeocode(unittest.TestCase):
    def setUp(self):
        # Using LA and SF coordinates from HaversineUtilTest
        self.la_long = -118.24
        self.la_lat = 34.05
        self.sf_long = -122.45
        self.sf_lat = 37.73
        
        self.la_point = geocode.EarthPoint(self.la_long, self.la_lat)
        self.sf_point = geocode.EarthPoint(self.sf_long, self.sf_lat)

    def test_get_distance_should_calculate_correctly(self):
        distance = geocode.get_distance_meters(self.la_point, self.sf_point)
        expected = 557787  # From HaversineUtilTest
        
        # Test within 5% of expected value
        difference = abs(distance - expected)
        tolerance = expected * 0.05
        
        self.assertLess(difference, tolerance, "Distance should be within 5% of expected value")

    def test_get_distance_should_return_zero_for_same_point(self):
        point = geocode.EarthPoint(1.23, 1.23)
        distance = geocode.get_distance_meters(point, point)
        self.assertAlmostEqual(distance, 0, places=2)

    def test_get_at_distance_from_should_move_north_correctly(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "N")
        
        self.assertGreater(result.get_latitude(), start.get_latitude())
        self.assertAlmostEqual(result.get_longitude(), start.get_longitude(), places=2)

    def test_get_at_distance_from_should_move_south_correctly(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "S")
        
        self.assertLess(result.get_latitude(), start.get_latitude())
        self.assertAlmostEqual(result.get_longitude(), start.get_longitude(), places=2)

    def test_get_at_distance_from_should_move_east_correctly(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "E")
        
        self.assertGreater(result.get_longitude(), start.get_longitude())
        self.assertAlmostEqual(result.get_latitude(), start.get_latitude(), places=2)

    def test_get_at_distance_from_should_move_west_correctly(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "W")
        
        self.assertLess(result.get_longitude(), start.get_longitude())
        self.assertAlmostEqual(result.get_latitude(), start.get_latitude(), places=2)

    def test_get_at_distance_from_invalid_direction(self):
        start = geocode.EarthPoint(-122.45, 37.73)
        with self.assertRaises(ValueError):
            geocode.get_at_distance_from(start, 5000, "X")

    def test_encodes_and_decodes_in_agreement_north(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "N")
        
        delta = abs(geocode.get_distance_meters(start, result) - 5000)
        self.assertLess(delta, 0.0001)

    def test_encodes_and_decodes_in_agreement_east(self):
        start = geocode.EarthPoint(-122.45, 37.73)  # SF coordinates
        result = geocode.get_at_distance_from(start, 5000, "E")
        
        delta = abs(geocode.get_distance_meters(start, result) - 5000)
        self.assertLess(delta, 0.0001)

if __name__ == '__main__':
    unittest.main()
