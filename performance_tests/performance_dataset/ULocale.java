static ULocale[] parseAcceptLanguage(String acceptLanguage, boolean isLenient)
throws ParseException {
    class ULocaleAcceptLanguageQ implements Comparable<ULocaleAcceptLanguageQ> {
        private double q;
        private double serial;
        public ULocaleAcceptLanguageQ(double theq, int theserial) {
            q = theq;
            serial = theserial;
        }
        public int compareTo(ULocaleAcceptLanguageQ other) {
            if (q > other.q) { // reverse - to sort in descending order
                return -1;
            } else if (q < other.q) {
                return 1;
            }
            if (serial < other.serial) {
                return -1;
            } else if (serial > other.serial) {
                return 1;
            } else {
                return 0; // same object
            }
        }
    }
    // parse out the acceptLanguage into an array
    TreeMap<ULocaleAcceptLanguageQ, ULocale> map =
        new TreeMap<ULocaleAcceptLanguageQ, ULocale>();
    StringBuilder languageRangeBuf = new StringBuilder();
    StringBuilder qvalBuf = new StringBuilder();
    int state = 0;
    acceptLanguage += ","; // append comma to simplify the parsing code
    int n;
    boolean subTag = false;
    boolean q1 = false;
    for (n = 0; n < acceptLanguage.length(); n++) {
        boolean gotLanguageQ = false;
        char c = acceptLanguage.charAt(n);
        switch (state) {
        case 0: // before language-range start
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                // in language-range
                languageRangeBuf.append(c);
                state = 1;
                subTag = false;
            } else if (c == '*') {
                languageRangeBuf.append(c);
                state = 2;
            } else if (c != ' ' && c != '\t') {
                // invalid character
                state = -1;
            }
            break;
        case 1: // in language-range
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                languageRangeBuf.append(c);
            } else if (c == '-') {
                subTag = true;
                languageRangeBuf.append(c);
            } else if (c == '_') {
                if (isLenient) {
                    subTag = true;
                    languageRangeBuf.append(c);
                } else {
                    state = -1;
                }
            } else if ('0' <= c && c <= '9') {
                if (subTag) {
                    languageRangeBuf.append(c);
                } else {
                    // DIGIT is allowed only in language sub tag
                    state = -1;
                }
            } else if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c == ' ' || c == '\t') {
                // language-range end
                state = 3;
            } else if (c == ';') {
                // before q
                state = 4;
            } else {
                // invalid character for language-range
                state = -1;
            }
            break;
        case 2: // saw wild card range
            if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c == ' ' || c == '\t') {
                // language-range end
                state = 3;
            } else if (c == ';') {
                // before q
                state = 4;
            } else {
                // invalid
                state = -1;
            }
            break;
        case 3: // language-range end
            if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c == ';') {
                // before q
                state =4;
            } else if (c != ' ' && c != '\t') {
                // invalid
                state = -1;
            }
            break;
        case 4: // before q
            if (c == 'q') {
                // before equal
                state = 5;
            } else if (c != ' ' && c != '\t') {
                // invalid
                state = -1;
            }
            break;
        case 5: // before equal
            if (c == '=') {
                // before q value
                state = 6;
            } else if (c != ' ' && c != '\t') {
                // invalid
                state = -1;
            }
            break;
        case 6: // before q value
            if (c == '0') {
                // q value start with 0
                q1 = false;
                qvalBuf.append(c);
                state = 7;
            } else if (c == '1') {
                // q value start with 1
                qvalBuf.append(c);
                state = 7;
            } else if (c == '.') {
                if (isLenient) {
                    qvalBuf.append(c);
                    state = 8;
                } else {
                    state = -1;
                }
            } else if (c != ' ' && c != '\t') {
                // invalid
                state = -1;
            }
            break;
        case 7: // q value start
            if (c == '.') {
                // before q value fraction part
                qvalBuf.append(c);
                state = 8;
            } else if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c == ' ' || c == '\t') {
                // after q value
                state = 10;
            } else {
                // invalid
                state = -1;
            }
            break;
        case 8: // before q value fraction part
            if ('0' <= c || c <= '9') {
                if (q1 && c != '0' && !isLenient) {
                    // if q value starts with 1, the fraction part must be 0
                    state = -1;
                } else {
                    // in q value fraction part
                    qvalBuf.append(c);
                    state = 9;
                }
            } else {
                // invalid
                state = -1;
            }
            break;
        case 9: // in q value fraction part
            if ('0' <= c && c <= '9') {
                if (q1 && c != '0') {
                    // if q value starts with 1, the fraction part must be 0
                    state = -1;
                } else {
                    qvalBuf.append(c);
                }
            } else if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c == ' ' || c == '\t') {
                // after q value
                state = 10;
            } else {
                // invalid
                state = -1;
            }
            break;
        case 10: // after q value
            if (c == ',') {
                // language-q end
                gotLanguageQ = true;
            } else if (c != ' ' && c != '\t') {
                // invalid
                state = -1;
            }
            break;
        }
        if (state == -1) {
            // error state
            throw new ParseException("Invalid Accept-Language", n);
        }
        if (gotLanguageQ) {
            double q = 1.0;
            if (qvalBuf.length() != 0) {
                try {
                    q = Double.parseDouble(qvalBuf.toString());
                } catch (NumberFormatException nfe) {
                    // Already validated, so it should never happen
                    q = 1.0;
                }
                if (q > 1.0) {
                    q = 1.0;
                }
            }
            if (languageRangeBuf.charAt(0) != '*') {
                int serial = map.size();
                ULocaleAcceptLanguageQ entry = new ULocaleAcceptLanguageQ(q, serial);
                // sort in reverse order..   1.0, 0.9, 0.8 .. etc
                map.put(entry, new ULocale(canonicalize(languageRangeBuf.toString())));
            }
            // reset buffer and parse state
            languageRangeBuf.setLength(0);
            qvalBuf.setLength(0);
            state = 0;
        }
    }
    if (state != 0) {
        // Well, the parser should handle all cases.  So just in case.
        throw new ParseException("Invalid AcceptlLanguage", n);
    }
    // pull out the map
    ULocale acceptList[] = map.values().toArray(new ULocale[map.size()]);
    return acceptList;
}