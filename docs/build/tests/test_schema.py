"""Tests for the authoring contract."""

from pathlib import Path

import pytest
from pydantic import ValidationError

from joshdocs.schema import (
    DEFAULT_SEED,
    Expect,
    ExportSlot,
    FrontMatter,
    Kind,
    Status,
    locate_key,
    problems_from_validation_error,
)

SIDECAR = Path("docs/src/recipes/x.md")


def minimal(**overrides):
    fields = {"kind": "recipe", "title": "A recipe"}
    fields.update(overrides)
    return FrontMatter.model_validate(fields)


def test_minimal_front_matter_fills_in_defaults():
    front = minimal()
    assert front.runnable is True
    assert front.asserts is False
    assert front.seed == DEFAULT_SEED
    assert front.expect is Expect.VALID
    assert front.status is Status.ACTIVE
    assert front.exports == []


def test_reference_is_not_runnable_by_default():
    assert minimal(kind="reference").runnable is False
    assert minimal(kind="guide").runnable is True


def test_explicit_runnable_overrides_the_kind_default():
    assert minimal(kind="reference", runnable=True).runnable is True


def test_assert_is_read_from_the_yaml_keyword():
    front = FrontMatter.model_validate({"kind": "recipe", "title": "t", "assert": True})
    assert front.asserts is True


def test_unknown_fields_are_rejected():
    with pytest.raises(ValidationError) as caught:
        minimal(runnabel=True)
    assert caught.value.errors()[0]["type"] == "extra_forbidden"


def test_exports_are_parsed_as_slots():
    front = minimal(exports=["patch", "meta"], simulation="Main")
    assert front.exports == [ExportSlot.PATCH, ExportSlot.META]


def test_exports_require_a_simulation_name():
    with pytest.raises(ValidationError, match="requires simulation"):
        minimal(exports=["patch"])


def test_exports_require_runnable():
    with pytest.raises(ValidationError, match="exports: requires runnable"):
        minimal(kind="reference", exports=["patch"], simulation="Main")


def test_assert_requires_runnable():
    with pytest.raises(ValidationError, match="assert: true requires runnable"):
        FrontMatter.model_validate(
            {"kind": "reference", "title": "t", "assert": True}
        )


def test_reserved_requires_a_reason():
    with pytest.raises(ValidationError, match="requires reason"):
        minimal(status="reserved")
    assert minimal(status="reserved", reason="config syntax is not implemented").reason


def test_reason_without_reserved_is_rejected():
    with pytest.raises(ValidationError, match="only applies to status: reserved"):
        minimal(reason="stray")


def test_parse_error_cannot_be_runnable():
    with pytest.raises(ValidationError, match="cannot be runnable"):
        minimal(expect="parse-error")
    assert minimal(kind="reference", expect="parse-error").expect is Expect.PARSE_ERROR


def test_parse_error_cannot_assert():
    with pytest.raises(ValidationError, match="cannot carry assert"):
        FrontMatter.model_validate(
            {"kind": "reference", "title": "t", "expect": "parse-error", "assert": True}
        )


def test_ids_are_restricted_to_url_safe_characters():
    assert minimal(id="wind-dispersal").id == "wind-dispersal"
    assert minimal(id="test_entity_update_basic").id == "test_entity_update_basic"
    with pytest.raises(ValidationError, match="not a valid id"):
        minimal(id="Wind Dispersal")


def test_destination_must_stay_inside_the_output_tree():
    assert minimal(destination="/recipes/dispersal/").destination == "recipes/dispersal"
    with pytest.raises(ValidationError, match="without '..'"):
        minimal(destination="../secrets")


def test_blank_title_is_rejected():
    with pytest.raises(ValidationError, match="must not be empty"):
        minimal(title="   ")


def test_negative_seed_is_rejected():
    with pytest.raises(ValidationError, match="must not be negative"):
        minimal(seed=-1)


def test_blank_list_entries_are_rejected():
    with pytest.raises(ValidationError, match="must not be empty"):
        minimal(tags=["spatial", " "])


def test_locate_key_finds_the_line_in_the_file():
    raw = "title: t\norder: thirty\nkind: recipe"
    # Line 1 of the file is the opening `---`, so `order` is on line 3.
    assert locate_key(raw, "order") == 3
    assert locate_key(raw, "seed") is None


def test_bad_integer_reports_the_key_the_line_and_the_value():
    with pytest.raises(ValidationError) as caught:
        minimal(order="thirty")
    problems = problems_from_validation_error(
        caught.value, SIDECAR, "kind: recipe\ntitle: t\norder: thirty"
    )
    assert len(problems) == 1
    assert problems[0].message == "order: expected an integer, got 'thirty'"
    assert problems[0].line == 4
    assert problems[0].format() == (
        "docs/src/recipes/x.md:4\n  order: expected an integer, got 'thirty'"
    )


def test_missing_required_field_is_named():
    with pytest.raises(ValidationError) as caught:
        FrontMatter.model_validate({"kind": "recipe"})
    problems = problems_from_validation_error(caught.value, SIDECAR, "kind: recipe", 2)
    assert problems[0].message == "title: required field is missing"
    assert problems[0].line == 2


def test_bad_enum_value_lists_what_was_expected():
    with pytest.raises(ValidationError) as caught:
        minimal(kind="tutorial")
    problems = problems_from_validation_error(caught.value, SIDECAR, "kind: tutorial")
    assert "expected" in problems[0].message
    assert "'tutorial'" in problems[0].message


def test_assert_errors_are_reported_under_the_yaml_key():
    with pytest.raises(ValidationError) as caught:
        FrontMatter.model_validate({"kind": "recipe", "title": "t", "assert": "yes please"})
    problems = problems_from_validation_error(
        caught.value, SIDECAR, "kind: recipe\ntitle: t\nassert: yes please"
    )
    assert problems[0].message.startswith("assert: expected true or false")
    assert problems[0].line == 4


def test_every_kind_has_a_runnable_default():
    for kind in Kind:
        assert isinstance(minimal(kind=kind.value, title="t").runnable, bool)
