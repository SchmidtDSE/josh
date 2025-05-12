
import unittest
from .parse import parse_engine_value_string, parse_start_end_string

class TestParse(unittest.TestCase):
    def test_parse_engine_value_string(self):
        test_string = "30 m"
        result = parse_engine_value_string(test_string)
        self.assertEqual(result.get_value(), 30.0)
        self.assertEqual(result.get_units(), "m")

    def test_parse_start_end_string_latitude_first(self):
        test_string = "36.51947777043374 degrees latitude, -118.67203360913730 degrees longitude"
        result = parse_start_end_string(test_string)
        self.assertEqual(result.get_longitude().get_value(), -118.67203360913730)
        self.assertEqual(result.get_longitude().get_units(), "degrees")
        self.assertEqual(result.get_latitude().get_value(), 36.51947777043374)
        self.assertEqual(result.get_latitude().get_units(), "degrees")

    def test_parse_start_end_string_longitude_first(self):
        test_string = "-118.67203360913730 degrees longitude, 36.51947777043374 degrees latitude"
        result = parse_start_end_string(test_string)
        self.assertEqual(result.get_longitude().get_value(), -118.67203360913730)
        self.assertEqual(result.get_longitude().get_units(), "degrees")
        self.assertEqual(result.get_latitude().get_value(), 36.51947777043374)
        self.assertEqual(result.get_latitude().get_units(), "degrees")

if __name__ == '__main__':
    unittest.main()
