"""Tests for sidecar loading."""

import pytest

from joshdocs.sidecar import SidecarError, extract_raw_frontmatter, load_sidecar

SIDECAR = """---
kind: recipe
title: A recipe
---

Prose about the recipe.
"""


def write(tmp_path, text, name="unit.md"):
    path = tmp_path / name
    path.write_text(text, encoding="utf-8")
    return path


def test_loads_front_matter_and_body(tmp_path):
    sidecar = load_sidecar(write(tmp_path, SIDECAR))
    assert sidecar.metadata == {"kind": "recipe", "title": "A recipe"}
    assert sidecar.body.strip() == "Prose about the recipe."


def test_raw_front_matter_excludes_the_delimiters(tmp_path):
    sidecar = load_sidecar(write(tmp_path, SIDECAR))
    assert sidecar.raw_frontmatter == "kind: recipe\ntitle: A recipe"


def test_missing_front_matter_names_the_fix(tmp_path):
    with pytest.raises(SidecarError) as caught:
        load_sidecar(write(tmp_path, "Just prose.\n"))
    assert "must open with a '---' line" in caught.value.problem.message
    assert caught.value.problem.line == 1


def test_unclosed_front_matter_is_reported(tmp_path):
    with pytest.raises(SidecarError):
        load_sidecar(write(tmp_path, "---\nkind: recipe\n\nprose\n"))


def test_invalid_yaml_reports_a_file_line(tmp_path):
    broken = "---\nkind: recipe\ntags: [unclosed\ntitle: t\n---\n"
    with pytest.raises(SidecarError) as caught:
        load_sidecar(write(tmp_path, broken))
    problem = caught.value.problem
    assert "not valid YAML" in problem.message
    # The unterminated list opens on line 3 of the file.
    assert problem.line is not None
    assert problem.line >= 3


def test_scalar_front_matter_is_rejected(tmp_path):
    with pytest.raises(SidecarError, match="must be a mapping"):
        load_sidecar(write(tmp_path, "---\njust a string\n---\n"))


def test_unreadable_file_is_reported(tmp_path):
    with pytest.raises(SidecarError, match="could not be read"):
        load_sidecar(tmp_path / "absent.md")


def test_extract_raw_front_matter_without_a_block():
    assert extract_raw_frontmatter("no front matter") == ""
    assert extract_raw_frontmatter("") == ""
