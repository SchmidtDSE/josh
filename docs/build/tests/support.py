"""Helpers shared by the docs-builder tests."""

#: Stand-in for the one field almost no test is about. Every sidecar has to carry a description --
#: an index of bare titles is what that requirement exists to prevent -- but repeating a real one in
#: ninety fixtures would bury the field each of them is actually testing.
DESCRIPTION = "What this unit is for."


def with_description(front_matter: str) -> str:
    """Return front-matter carrying a description, adding the placeholder when it declares none.

    Args:
        front_matter: The YAML block a test wrote, without its delimiters.

    Returns:
        The same block, with a description appended if it had none.
    """
    if "description:" in front_matter:
        return front_matter
    return f"{front_matter}\ndescription: {DESCRIPTION}"
