protected boolean parseTag(int previousPosition) throws InvalidInputException {
    // Read tag name
    int currentPosition = this.index;
    int token = readTokenAndConsume();
    char[] tagName = CharOperation.NO_CHAR;
    if (currentPosition == this.scanner.startPosition) {
        this.tagSourceStart = this.scanner.getCurrentTokenStartPosition();
        this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
        tagName = this.scanner.getCurrentIdentifierSource();
    } else {
        this.tagSourceEnd = currentPosition-1;
    }
    // Try to get tag name other than java identifier
    // (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51660)
    if (this.scanner.currentCharacter != ' ' && !ScannerHelper.isWhitespace(this.scanner.currentCharacter)) {
        tagNameToken: while (token != TerminalTokens.TokenNameEOF && this.index < this.scanner.eofPosition) {
            int length = tagName.length;
            // !, ", #, %, &, ', -, :, <, >, * chars and spaces are not allowed in tag names
            switch (this.scanner.currentCharacter) {
            case '}':
            case '*': // break for '*' as this is perhaps the end of comment (bug 65288)
            case '!':
            case '#':
            case '%':
            case '&':
            case '\'':
            case '"':
            case ':':
            case '<':
            case '>':
                break tagNameToken;
            case '-': // allowed in tag names as this character is often used in doclets (bug 68087)
                System.arraycopy(tagName, 0, tagName = new char[length+1], 0, length);
                tagName[length] = this.scanner.currentCharacter;
                break;
            default:
                if (this.scanner.currentCharacter == ' ' || ScannerHelper.isWhitespace(this.scanner.currentCharacter)) {
                    break tagNameToken;
                }
                token = readTokenAndConsume();
                char[] ident = this.scanner.getCurrentIdentifierSource();
                System.arraycopy(tagName, 0, tagName = new char[length+ident.length], 0, length);
                System.arraycopy(ident, 0, tagName, length, ident.length);
                break;
            }
            this.tagSourceEnd = this.scanner.getCurrentTokenEndPosition();
            this.scanner.getNextChar();
            this.index = this.scanner.currentPosition;
        }
    }
    int length = tagName.length;
    this.index = this.tagSourceEnd+1;
    this.scanner.currentPosition = this.tagSourceEnd+1;
    this.tagSourceStart = previousPosition;
    // tage name may be empty (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=125903)
    if (tagName.length == 0) {
        return false;
    }
    // Decide which parse to perform depending on tag name
    this.tagValue = NO_TAG_VALUE;
    boolean valid = true;
    switch (token) {
    case TerminalTokens.TokenNameIdentifier :
        switch (tagName[0]) {
        case 'c':
            if (length == TAG_CATEGORY_LENGTH && CharOperation.equals(TAG_CATEGORY, tagName)) {
                this.tagValue = TAG_CATEGORY_VALUE;
                valid = parseIdentifierTag(false); // TODO (frederic) reconsider parameter value when @category will be significant in spec
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        case 'd':
            if (length == TAG_DEPRECATED_LENGTH && CharOperation.equals(TAG_DEPRECATED, tagName)) {
                this.deprecated = true;
                this.tagValue = TAG_DEPRECATED_VALUE;
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
            }
            createTag();
            break;
        case 'i':
            if (length == TAG_INHERITDOC_LENGTH && CharOperation.equals(TAG_INHERITDOC, tagName)) {
                if (this.reportProblems) {
                    recordInheritedPosition((((long) this.tagSourceStart) << 32) + this.tagSourceEnd);
                }
                this.tagValue = TAG_INHERITDOC_VALUE;
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
            }
            createTag();
            break;
        case 'p':
            if (length == TAG_PARAM_LENGTH && CharOperation.equals(TAG_PARAM, tagName)) {
                this.tagValue = TAG_PARAM_VALUE;
                valid = parseParam();
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        case 'e':
            if (length == TAG_EXCEPTION_LENGTH && CharOperation.equals(TAG_EXCEPTION, tagName)) {
                this.tagValue = TAG_EXCEPTION_VALUE;
                valid = parseThrows();
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        case 's':
            if (length == TAG_SEE_LENGTH && CharOperation.equals(TAG_SEE, tagName)) {
                this.tagValue = TAG_SEE_VALUE;
                if (this.inlineTagStarted) {
                    // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53290
                    // Cannot have @see inside inline comment
                    valid = false;
                } else {
                    valid = parseReference();
                }
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        case 'l':
            if (length == TAG_LINK_LENGTH && CharOperation.equals(TAG_LINK, tagName)) {
                this.tagValue = TAG_LINK_VALUE;
            } else if (length == TAG_LINKPLAIN_LENGTH && CharOperation.equals(TAG_LINKPLAIN, tagName)) {
                this.tagValue = TAG_LINKPLAIN_VALUE;
            }
            if (this.tagValue != NO_TAG_VALUE)  {
                if (this.inlineTagStarted) {
                    valid = parseReference();
                } else {
                    // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53290
                    // Cannot have @link outside inline comment
                    valid = false;
                }
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        case 'v':
            if (this.sourceLevel >= ClassFileConstants.JDK1_5 && length == TAG_VALUE_LENGTH && CharOperation.equals(TAG_VALUE, tagName)) {
                this.tagValue = TAG_VALUE_VALUE;
                if (this.inlineTagStarted) {
                    valid = parseReference();
                } else {
                    valid = false;
                }
            } else {
                this.tagValue = TAG_OTHERS_VALUE;
                createTag();
            }
            break;
        default:
            this.tagValue = TAG_OTHERS_VALUE;
            createTag();
        }
        break;
    case TerminalTokens.TokenNamereturn :
        this.tagValue = TAG_RETURN_VALUE;
        valid = parseReturn();
        break;
    case TerminalTokens.TokenNamethrows :
        this.tagValue = TAG_THROWS_VALUE;
        valid = parseThrows();
        break;
    case TerminalTokens.TokenNameabstract:
    case TerminalTokens.TokenNameassert:
    case TerminalTokens.TokenNameboolean:
    case TerminalTokens.TokenNamebreak:
    case TerminalTokens.TokenNamebyte:
    case TerminalTokens.TokenNamecase:
    case TerminalTokens.TokenNamecatch:
    case TerminalTokens.TokenNamechar:
    case TerminalTokens.TokenNameclass:
    case TerminalTokens.TokenNamecontinue:
    case TerminalTokens.TokenNamedefault:
    case TerminalTokens.TokenNamedo:
    case TerminalTokens.TokenNamedouble:
    case TerminalTokens.TokenNameelse:
    case TerminalTokens.TokenNameextends:
    case TerminalTokens.TokenNamefalse:
    case TerminalTokens.TokenNamefinal:
    case TerminalTokens.TokenNamefinally:
    case TerminalTokens.TokenNamefloat:
    case TerminalTokens.TokenNamefor:
    case TerminalTokens.TokenNameif:
    case TerminalTokens.TokenNameimplements:
    case TerminalTokens.TokenNameimport:
    case TerminalTokens.TokenNameinstanceof:
    case TerminalTokens.TokenNameint:
    case TerminalTokens.TokenNameinterface:
    case TerminalTokens.TokenNamelong:
    case TerminalTokens.TokenNamenative:
    case TerminalTokens.TokenNamenew:
    case TerminalTokens.TokenNamenull:
    case TerminalTokens.TokenNamepackage:
    case TerminalTokens.TokenNameprivate:
    case TerminalTokens.TokenNameprotected:
    case TerminalTokens.TokenNamepublic:
    case TerminalTokens.TokenNameshort:
    case TerminalTokens.TokenNamestatic:
    case TerminalTokens.TokenNamestrictfp:
    case TerminalTokens.TokenNamesuper:
    case TerminalTokens.TokenNameswitch:
    case TerminalTokens.TokenNamesynchronized:
    case TerminalTokens.TokenNamethis:
    case TerminalTokens.TokenNamethrow:
    case TerminalTokens.TokenNametransient:
    case TerminalTokens.TokenNametrue:
    case TerminalTokens.TokenNametry:
    case TerminalTokens.TokenNamevoid:
    case TerminalTokens.TokenNamevolatile:
    case TerminalTokens.TokenNamewhile:
    case TerminalTokens.TokenNameenum :
    case TerminalTokens.TokenNameconst :
    case TerminalTokens.TokenNamegoto :
        this.tagValue = TAG_OTHERS_VALUE;
        createTag();
        break;
    }
    this.textStart = this.index;
    return valid;
}