"""Tests for synthesizing units from conformance test headers."""

from pathlib import Path

from joshdocs.conformance import header_problems, parse_header, title_from_stem

HEADER = """# @category: core
# @subcategory: entity_overwrite
# @priority: critical
# @description: update merges onto a prior same-named entity - the handler it redeclares
#   overrides the original, while every other handler survives unchanged

start simulation EntityUpdateBasic
# @priority: bogus
end simulation
"""


def test_parses_every_known_tag():
    header = parse_header(HEADER)
    assert header.category == "core"
    assert header.subcategory == "entity_overwrite"
    assert header.priority == "critical"


def test_joins_a_wrapped_description():
    header = parse_header(HEADER)
    assert header.description == (
        "update merges onto a prior same-named entity - the handler it redeclares overrides the "
        "original, while every other handler survives unchanged"
    )


def test_stops_at_the_first_line_of_code():
    # The bogus priority sits below `start simulation`, so it is never read -- matching
    # TestMetadata.parse, which stops at the first non-comment line.
    assert parse_header(HEADER).priority == "critical"


def test_ignores_unknown_tags():
    header = parse_header("# @category: core\n# @author: someone\n# @subcategory: s\n")
    assert header.category == "core"
    assert header.subcategory == "s"


def test_a_continuation_after_an_unknown_tag_is_dropped():
    header = parse_header("# @description: real\n# @author: someone\n#   more about the author\n")
    assert header.description == "real"


def test_blank_comment_lines_do_not_end_the_header():
    header = parse_header("# @category: core\n#\n# @subcategory: s\n\nstart simulation Main\n")
    assert header.subcategory == "s"


def test_missing_tags_are_reported():
    problems = header_problems(Path("t.josh"), parse_header("# @category: core\n"))
    messages = [problem.message for problem in problems]
    assert "conformance header is missing '# @subcategory:'" in messages
    assert "conformance header is missing '# @priority:'" in messages
    assert "conformance header is missing '# @description:'" in messages
    assert "conformance header is missing '# @category:'" not in messages


def test_unknown_priority_is_reported():
    header = parse_header(
        "# @category: c\n# @subcategory: s\n# @priority: urgent\n# @description: d\n"
    )
    problems = header_problems(Path("t.josh"), header)
    assert len(problems) == 1
    assert "'urgent' is not one of" in problems[0].message


def test_legacy_normal_priority_is_accepted():
    header = parse_header(
        "# @category: c\n# @subcategory: s\n# @priority: normal\n# @description: d\n"
    )
    assert header_problems(Path("t.josh"), header) == []


def test_complete_header_has_no_problems():
    assert header_problems(Path("t.josh"), parse_header(HEADER)) == []


def test_title_is_read_from_the_filename_not_the_description():
    # The description is a sentence about what the test proves; a heading wants the short form.
    assert title_from_stem("test_entity_update_basic") == "Entity update basic"
    assert title_from_stem("test_external_jshd_large") == "External jshd large"
    assert title_from_stem("units_custom") == "Units custom"
