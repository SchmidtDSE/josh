return getAttributeNames().stream()
  .map(this::getAttributeValue)
  .filter(Optional::isPresent)
  .map(Optional::get)
  .filter((x) -> x.getLanguageType().containsAttributes())
  .map((x) -> x.getAsMutableEntity());