protected boolean commentParse() {
    boolean validComment = true;
    try {
        // Init local variables
        this.astLengthPtr = -1;
        this.astPtr = -1;
        this.identifierPtr = -1;
        this.currentTokenType = -1;
        setInlineTagStarted(false);
        this.inlineTagStart = -1;
        this.lineStarted = false;
        this.returnStatement = null;
        this.inheritedPositions = null;
        this.lastBlockTagValue = NO_TAG_VALUE;
        this.deprecated = false;
        this.lastLinePtr = getLineNumber(this.javadocEnd);
        this.textStart = -1;
        this.abort = false;
        char previousChar = 0;
        int invalidTagLineEnd = -1;
        int invalidInlineTagLineEnd = -1;
        boolean lineHasStar = true;
        boolean verifText = (this.kind & TEXT_VERIF) != 0;
        boolean isDomParser = (this.kind & DOM_PARSER) != 0;
        boolean isFormatterParser = (this.kind & FORMATTER_COMMENT_PARSER) != 0;
        int lastStarPosition = -1;
        // Init scanner position
        this.linePtr = getLineNumber(this.firstTagPosition);
        int realStart = this.linePtr==1 ? this.javadocStart : this.scanner.getLineEnd(this.linePtr-1)+1;
        if (realStart < this.javadocStart) realStart = this.javadocStart;
        this.scanner.resetTo(realStart, this.javadocEnd);
        this.index = realStart;
        if (realStart == this.javadocStart) {
            readChar(); // starting '/'
            readChar(); // first '*'
        }
        int previousPosition = this.index;
        char nextCharacter = 0;
        if (realStart == this.javadocStart) {
            nextCharacter = readChar(); // second '*'
            while (peekChar() == '*') {
                nextCharacter = readChar(); // read all contiguous '*'
            }
            this.javadocTextStart = this.index;
        }
        this.lineEnd = (this.linePtr == this.lastLinePtr) ? this.javadocEnd: this.scanner.getLineEnd(this.linePtr) - 1;
        this.javadocTextEnd = this.javadocEnd - 2; // supposed text end, it will be refined later...
        // Loop on each comment character
        int textEndPosition = -1;
        while (!this.abort && this.index < this.javadocEnd) {
            // Store previous position and char
            previousPosition = this.index;
            previousChar = nextCharacter;
            // Calculate line end (cannot use this.scanner.linePtr as scanner does not parse line ends again)
            if (this.index > (this.lineEnd+1)) {
                updateLineEnd();
            }
            // Read next char only if token was consumed
            if (this.currentTokenType < 0) {
                nextCharacter = readChar(); // consider unicodes
            } else {
                previousPosition = this.scanner.getCurrentTokenStartPosition();
                switch (this.currentTokenType) {
                case TerminalTokens.TokenNameRBRACE:
                    nextCharacter = '}';
                    break;
                case TerminalTokens.TokenNameMULTIPLY:
                    nextCharacter = '*';
                    break;
                default:
                    nextCharacter = this.scanner.currentCharacter;
                }
                consumeToken();
            }
            // Consume rules depending on the read character
            switch (nextCharacter) {
            case '@' :
                // Start tag parsing only if we are on line beginning or at inline tag beginning
                if ((!this.lineStarted || previousChar == '{')) {
                    if (this.inlineTagStarted) {
                        setInlineTagStarted(false);
                        // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53279
                        // Cannot have @ inside inline comment
                        if (this.reportProblems) {
                            int end = previousPosition<invalidInlineTagLineEnd ? previousPosition : invalidInlineTagLineEnd;
                            this.sourceParser.problemReporter().javadocUnterminatedInlineTag(this.inlineTagStart, end);
                        }
                        validComment = false;
                        if (this.textStart != -1 && this.textStart < textEndPosition) {
                            pushText(this.textStart, textEndPosition);
                        }
                        if (isDomParser || isFormatterParser) {
                            refreshInlineTagPosition(textEndPosition);
                        }
                    }
                    if (previousChar == '{') {
                        if (this.textStart != -1) {
                            if (this.textStart < textEndPosition) {
                                pushText(this.textStart, textEndPosition);
                            }
                        }
                        setInlineTagStarted(true);
                        invalidInlineTagLineEnd = this.lineEnd;
                    } else if (this.textStart != -1 && this.textStart < invalidTagLineEnd) {
                        pushText(this.textStart, invalidTagLineEnd);
                    }
                    this.scanner.resetTo(this.index, this.javadocEnd);
                    this.currentTokenType = -1; // flush token cache at line begin
                    try {
                        if (!parseTag(previousPosition)) {
                            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51600
                            // do not stop the inline tag when error is encountered to get text after
                            validComment = false;
                            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=51600
                            // for DOM AST node, store tag as text in case of invalid syntax
                            if (isDomParser) {
                                createTag();
                            }
                            this.textStart = this.tagSourceEnd+1;
                            invalidTagLineEnd  = this.lineEnd;
                            textEndPosition = this.index;
                        }
                    } catch (InvalidInputException e) {
                        consumeToken();
                    }
                } else {
                    textEndPosition = this.index;
                    if (verifText && this.tagValue == TAG_RETURN_VALUE && this.returnStatement != null) {
                        refreshReturnStatement();
                    } else if (isFormatterParser) {
                        if (this.textStart == -1) this.textStart = previousPosition;
                    }
                }
                this.lineStarted = true;
                break;
            case '\r':
            case '\n':
                if (this.lineStarted) {
                    if (isFormatterParser && !ScannerHelper.isWhitespace(previousChar)) {
                        textEndPosition = previousPosition;
                    }
                    if (this.textStart != -1 && this.textStart < textEndPosition) {
                        pushText(this.textStart, textEndPosition);
                    }
                }
                this.lineStarted = false;
                lineHasStar = false;
                // Fix bug 51650
                this.textStart = -1;
                break;
            case '}' :
                if (verifText && this.tagValue == TAG_RETURN_VALUE && this.returnStatement != null) {
                    refreshReturnStatement();
                }
                if (this.inlineTagStarted) {
                    textEndPosition = this.index - 1;
                    if (this.lineStarted && this.textStart != -1 && this.textStart < textEndPosition) {
                        pushText(this.textStart, textEndPosition);
                    }
                    refreshInlineTagPosition(previousPosition);
                    if (!isFormatterParser) this.textStart = this.index;
                    setInlineTagStarted(false);
                } else {
                    if (!this.lineStarted) {
                        this.textStart = previousPosition;
                    }
                }
                this.lineStarted = true;
                textEndPosition = this.index;
                break;
            case '{' :
                if (verifText && this.tagValue == TAG_RETURN_VALUE && this.returnStatement != null) {
                    refreshReturnStatement();
                }
                if (this.inlineTagStarted) {
                    setInlineTagStarted(false);
                    // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53279
                    // Cannot have opening brace in inline comment
                    if (this.reportProblems) {
                        int end = previousPosition<invalidInlineTagLineEnd ? previousPosition : invalidInlineTagLineEnd;
                        this.sourceParser.problemReporter().javadocUnterminatedInlineTag(this.inlineTagStart, end);
                    }
                    if (this.lineStarted && this.textStart != -1 && this.textStart < textEndPosition) {
                        pushText(this.textStart, textEndPosition);
                    }
                    refreshInlineTagPosition(textEndPosition);
                    textEndPosition = this.index;
                } else if (peekChar() != '@') {
                    if (this.textStart == -1) this.textStart = previousPosition;
                    textEndPosition = this.index;
                }
                if (!this.lineStarted) {
                    this.textStart = previousPosition;
                }
                this.lineStarted = true;
                this.inlineTagStart = previousPosition;
                break;
            case '*' :
                // Store the star position as text start while formatting
                lastStarPosition = previousPosition;
                if (previousChar != '*') {
                    this.starPosition = previousPosition;
                    if (isDomParser || isFormatterParser) {
                        if (lineHasStar) {
                            this.lineStarted = true;
                            if (this.textStart == -1) {
                                this.textStart = previousPosition;
                                if (this.index <= this.javadocTextEnd) textEndPosition = this.index;
                            }
                        }
                        if (!this.lineStarted) {
                            lineHasStar = true;
                        }
                    }
                }
                break;
            case '\u000c' :	/* FORM FEED               */
            case ' ' :			/* SPACE                   */
            case '\t' :			/* HORIZONTAL TABULATION   */
                // Do not include trailing spaces in text while formatting
                if (isFormatterParser) {
                    if (!ScannerHelper.isWhitespace(previousChar)) {
                        textEndPosition = previousPosition;
                    }
                } else if (this.lineStarted && isDomParser) {
                    textEndPosition = this.index;
                }
                break;
            case '/':
                if (previousChar == '*') {
                    // End of javadoc
                    break;
                }
            // $FALL-THROUGH$ - fall through default case
            default :
                if (isFormatterParser && nextCharacter == '<') {
                    // html tags are meaningful for formatter parser
                    int initialIndex = this.index;
                    this.scanner.resetTo(this.index, this.javadocEnd);
                    if (!ScannerHelper.isWhitespace(previousChar)) {
                        textEndPosition = previousPosition;
                    }
                    if (parseHtmlTag(previousPosition, textEndPosition)) {
                        break;
                    }
                    if (this.abort) return false;
                    // Wrong html syntax continue to process character normally
                    this.scanner.currentPosition = initialIndex;
                    this.index = initialIndex;
                }
                if (verifText && this.tagValue == TAG_RETURN_VALUE && this.returnStatement != null) {
                    refreshReturnStatement();
                }
                if (!this.lineStarted || this.textStart == -1) {
                    this.textStart = previousPosition;
                }
                this.lineStarted = true;
                textEndPosition = this.index;
                break;
            }
        }
        this.javadocTextEnd = this.starPosition-1;
        // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53279
        // Cannot leave comment inside inline comment
        if (this.inlineTagStarted) {
            if (this.reportProblems) {
                int end = this.javadocTextEnd<invalidInlineTagLineEnd ? this.javadocTextEnd : invalidInlineTagLineEnd;
                if (this.index >= this.javadocEnd) end = invalidInlineTagLineEnd;
                this.sourceParser.problemReporter().javadocUnterminatedInlineTag(this.inlineTagStart, end);
            }
            if (this.lineStarted && this.textStart != -1 && this.textStart < textEndPosition) {
                pushText(this.textStart, textEndPosition);
            }
            refreshInlineTagPosition(textEndPosition);
            setInlineTagStarted(false);
        } else if (this.lineStarted && this.textStart != -1 && this.textStart <= textEndPosition && (this.textStart < this.starPosition || this.starPosition == lastStarPosition)) {
            pushText(this.textStart, textEndPosition);
        }
        updateDocComment();
    } catch (Exception ex) {
        validComment = false;
    }
    return validComment;
}