#!/usr/bin/env python3
"""Parse JUnit XML test results and output failure details as markdown."""

import xml.etree.ElementTree as ET
import sys


def main():
    if len(sys.argv) < 2:
        print("Usage: parse_test_failures.py <xml_file>", file=sys.stderr)
        sys.exit(1)

    xml_path = sys.argv[1]
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for testcase in root.findall(".//testcase"):
            failure = testcase.find("failure")
            error = testcase.find("error")
            if failure is not None or error is not None:
                name = testcase.get("name", "unknown")
                print(f"### `{name}`")
                print()
                elem = failure if failure is not None else error
                message = elem.get("message", "")
                text = elem.text or ""
                if message:
                    print(f"**Error:** {message}")
                    print()
                if text.strip():
                    print("```")
                    print(text.strip()[:2000])
                    print("```")
                print()
    except Exception as e:
        print(f"Error parsing {xml_path}: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
