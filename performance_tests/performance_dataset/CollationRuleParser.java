@SuppressWarnings("fallthrough")
private int parseNextTokenInternal(boolean startofrules) throws ParseException {
    boolean variabletop = false;
    boolean top = false;
    boolean inchars = true;
    boolean inquote = false;
    boolean wasinquote = false;
    byte before = 0;
    boolean isescaped = false;
    int /*newcharslen = 0,*/ newextensionlen = 0;
    int /*charsoffset = 0,*/ extensionoffset = 0;
    int newstrength = TOKEN_UNSET_;
    initializeParsedToken();
    int limit = m_rules_.length();
    while (m_current_ < limit) {
        char ch = m_source_.charAt(m_current_);
        if (inquote) {
            if (ch == 0x0027) { // '\''
                inquote = false;
            } else {
                if ((m_parsedToken_.m_charsLen_ == 0) || inchars) {
                    if (m_parsedToken_.m_charsLen_ == 0) {
                        m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                    }
                    m_parsedToken_.m_charsLen_ ++;
                } else {
                    if (newextensionlen == 0) {
                        extensionoffset = m_extraCurrent_;
                    }
                    newextensionlen ++;
                }
            }
        } else if (isescaped) {
            isescaped = false;
            if (newstrength == TOKEN_UNSET_) {
                throwParseException(m_rules_, m_current_);
            }
            if (ch != 0 && m_current_ != limit) {
                if (inchars) {
                    if (m_parsedToken_.m_charsLen_ == 0) {
                        m_parsedToken_.m_charsOffset_ = m_current_;
                    }
                    m_parsedToken_.m_charsLen_ ++;
                } else {
                    if (newextensionlen == 0) {
                        extensionoffset = m_current_;
                    }
                    newextensionlen ++;
                }
            }
        } else {
            if (!PatternProps.isWhiteSpace(ch)) {
                // Sets the strength for this entry
                switch (ch) {
                case 0x003D : // '='
                    if (newstrength != TOKEN_UNSET_) {
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    // if we start with strength, we'll reset to top
                    if (startofrules == true) {
                        return resetToTop(top, variabletop, extensionoffset,
                                          newextensionlen, before);
                    }
                    newstrength = Collator.IDENTICAL;
                    if (m_source_.charAt(m_current_ + 1) == 0x002A) { // '*'
                        m_current_++;
                        m_isStarred_ = true;
                    }
                    break;
                case 0x002C : // ','
                    if (newstrength != TOKEN_UNSET_) {
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    // if we start with strength, we'll reset to top
                    if (startofrules == true) {
                        return resetToTop(top, variabletop, extensionoffset,
                                          newextensionlen, before);
                    }
                    newstrength = Collator.TERTIARY;
                    break;
                case 0x003B : // ';'
                    if (newstrength != TOKEN_UNSET_) {
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    //if we start with strength, we'll reset to top
                    if(startofrules == true) {
                        return resetToTop(top, variabletop, extensionoffset,
                                          newextensionlen, before);
                    }
                    newstrength = Collator.SECONDARY;
                    break;
                case 0x003C : // '<'
                    if (newstrength != TOKEN_UNSET_) {
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    // if we start with strength, we'll reset to top
                    if (startofrules == true) {
                        return resetToTop(top, variabletop, extensionoffset,
                                          newextensionlen, before);
                    }
                    // before this, do a scan to verify whether this is
                    // another strength
                    if (m_source_.charAt(m_current_ + 1) == 0x003C) {
                        m_current_ ++;
                        if (m_source_.charAt(m_current_ + 1) == 0x003C) {
                            m_current_ ++; // three in a row!
                            newstrength = Collator.TERTIARY;
                        } else { // two in a row
                            newstrength = Collator.SECONDARY;
                        }
                    } else { // just one
                        newstrength = Collator.PRIMARY;
                    }
                    if (m_source_.charAt(m_current_ + 1) == 0x002A) { // '*'
                        m_current_++;
                        m_isStarred_ = true;
                    }
                    break;
                case 0x0026 : // '&'
                    if (newstrength != TOKEN_UNSET_) {
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    newstrength = TOKEN_RESET_; // PatternEntry::RESET = 0
                    break;
                case 0x005b : // '['
                    // options - read an option, analyze it
                    m_optionEnd_ = m_rules_.indexOf(0x005d, m_current_);
                    if (m_optionEnd_ != -1) { // ']'
                        byte result = readAndSetOption();
                        m_current_ = m_optionEnd_;
                        if ((result & TOKEN_TOP_MASK_) != 0) {
                            if (newstrength == TOKEN_RESET_) {
                                doSetTop();
                                if (before != 0) {
                                    // This is a combination of before and
                                    // indirection like
                                    // '&[before 2][first regular]<b'
                                    m_source_.append((char)0x002d);
                                    m_source_.append((char)before);
                                    m_extraCurrent_ += 2;
                                    m_parsedToken_.m_charsLen_ += 2;
                                }
                                m_current_ ++;
                                return doEndParseNextToken(newstrength,
                                                           true,
                                                           extensionoffset,
                                                           newextensionlen,
                                                           variabletop, before);
                            } else {
                                throwParseException(m_rules_, m_current_);
                            }
                        } else if ((result & TOKEN_VARIABLE_TOP_MASK_) != 0) {
                            if (newstrength != TOKEN_RESET_
                                    && newstrength != TOKEN_UNSET_) {
                                variabletop = true;
                                m_parsedToken_.m_charsOffset_
                                    = m_extraCurrent_;
                                m_source_.append((char)0xFFFF);
                                m_extraCurrent_ ++;
                                m_current_ ++;
                                m_parsedToken_.m_charsLen_ = 1;
                                return doEndParseNextToken(newstrength,
                                                           top,
                                                           extensionoffset,
                                                           newextensionlen,
                                                           variabletop, before);
                            } else {
                                throwParseException(m_rules_, m_current_);
                            }
                        } else if ((result & TOKEN_BEFORE_) != 0) {
                            if (newstrength == TOKEN_RESET_) {
                                before = (byte)(result & TOKEN_BEFORE_);
                            } else {
                                throwParseException(m_rules_, m_current_);
                            }
                        }
                    }
                    break;
                case 0x002F : // '/'
                    wasinquote = false; // if we were copying source
                    // characters, we want to stop now
                    inchars = false; // we're now processing expansion
                    break;
                case 0x005C : // back slash for escaped chars
                    isescaped = true;
                    break;
                // found a quote, we're gonna start copying
                case 0x0027 : //'\''
                    if (newstrength == TOKEN_UNSET_) {
                        // quote is illegal until we have a strength
                        throwParseException(m_rules_, m_current_);
                    }
                    inquote = true;
                    if (inchars) { // we're doing characters
                        if (wasinquote == false) {
                            m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                        }
                        if (m_parsedToken_.m_charsLen_ != 0) {
                            // We are processing characters in quote together.
                            // Copy whatever is in the current token, so that
                            // the unquoted string can be appended to that.
                            m_source_.append(m_source_.substring(
                                                 m_current_ - m_parsedToken_.m_charsLen_,
                                                 m_current_));
                            m_extraCurrent_ += m_parsedToken_.m_charsLen_;
                        }
                        m_parsedToken_.m_charsLen_ ++;
                    } else { // we're doing an expansion
                        if (wasinquote == false) {
                            extensionoffset = m_extraCurrent_;
                        }
                        if (newextensionlen != 0) {
                            m_source_.append(m_source_.substring(
                                                 m_current_ - newextensionlen,
                                                 m_current_));
                            m_extraCurrent_ += newextensionlen;
                        }
                        newextensionlen ++;
                    }
                    wasinquote = true;
                    m_current_ ++;
                    ch = m_source_.charAt(m_current_);
                    if (ch == 0x0027) { // copy the double quote
                        m_source_.append(ch);
                        m_extraCurrent_ ++;
                        inquote = false;
                    }
                    break;
                // '@' is french only if the strength is not currently set
                // if it is, it's just a regular character in collation
                case 0x0040 : // '@'
                    if (newstrength == TOKEN_UNSET_) {
                        m_options_.m_isFrenchCollation_ = true;
                        break;
                    }
                // fall through
                case 0x007C : //|
                    // this means we have actually been reading prefix part
                    // we want to store read characters to the prefix part
                    // and continue reading the characters (proper way
                    // would be to restart reading the chars, but in that
                    // case we would have to complicate the token hasher,
                    // which I do not intend to play with. Instead, we will
                    // do prefixes when prefixes are due (before adding the
                    // elements).
                    m_parsedToken_.m_prefixOffset_
                        = m_parsedToken_.m_charsOffset_;
                    m_parsedToken_.m_prefixLen_
                        = m_parsedToken_.m_charsLen_;
                    if (inchars) { // we're doing characters
                        if (wasinquote == false) {
                            m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                        }
                        if (m_parsedToken_.m_charsLen_ != 0) {
                            String prefix = m_source_.substring(
                                                m_current_ - m_parsedToken_.m_charsLen_,
                                                m_current_);
                            m_source_.append(prefix);
                            m_extraCurrent_ += m_parsedToken_.m_charsLen_;
                        }
                        m_parsedToken_.m_charsLen_ ++;
                    }
                    wasinquote = true;
                    do {
                        m_current_ ++;
                        ch = m_source_.charAt(m_current_);
                        // skip whitespace between '|' and the character
                    } while (PatternProps.isWhiteSpace(ch));
                    break;
                case 0x002D : // '-', indicates a range.
                    if (newstrength != TOKEN_UNSET_) {
                        m_savedIsStarred_ = m_isStarred_;
                        return doEndParseNextToken(newstrength,
                                                   top,
                                                   extensionoffset,
                                                   newextensionlen,
                                                   variabletop, before);
                    }
                    m_isStarred_ = m_savedIsStarred_;
                    // Ranges are valid only in starred tokens.
                    if (!m_isStarred_) {
                        throwParseException(m_rules_, m_current_);
                    }
                    newstrength = m_parsedToken_.m_strength_;
                    m_inRange_ = true;
                    break;
                case 0x0023: // '#' // this is a comment, skip everything through the end of line
                    do {
                        m_current_ ++;
                        ch = m_source_.charAt(m_current_);
                    } while (!isCharNewLine(ch));
                    break;
                case 0x0021: // '!' // ignoring java set thai reordering
                    break;
                default :
                    if (newstrength == TOKEN_UNSET_) {
                        throwParseException(m_rules_, m_current_);
                    }
                    if (isSpecialChar(ch) && (inquote == false)) {
                        throwParseException(m_rules_, m_current_);
                    }
                    if (ch == 0x0000 && m_current_ + 1 == limit) {
                        break;
                    }
                    if (inchars) {
                        if (m_parsedToken_.m_charsLen_ == 0) {
                            m_parsedToken_.m_charsOffset_ = m_current_;
                        }
                        m_parsedToken_.m_charsLen_++;
                    } else {
                        if (newextensionlen == 0) {
                            extensionoffset = m_current_;
                        }
                        newextensionlen ++;
                    }
                    break;
                }
            }
        }
        if (wasinquote) {
            if (ch != 0x27) {
                m_source_.append(ch);
                m_extraCurrent_ ++;
            }
        }
        m_current_ ++;
    }
    return doEndParseNextToken(newstrength, top,
                               extensionoffset, newextensionlen,
                               variabletop, before);
}