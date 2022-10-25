public final UnicodeSet applyPattern(String pattern) {
    checkFrozen();
    return applyPattern(pattern, null, null, IGNORE_SPACE);
}