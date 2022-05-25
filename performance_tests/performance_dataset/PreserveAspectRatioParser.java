protected void parsePreserveAspectRatio()
throws ParseException, IOException {
    preserveAspectRatioHandler.startPreserveAspectRatio();
    align: switch (current) {
    case 'n':
        current = reader.read();
        if (current != 'o') {
            reportCharacterExpectedError( 'o',current );
            skipIdentifier();
            break align;
        }
        current = reader.read();
        if (current != 'n') {
            reportCharacterExpectedError( 'o',current );
            skipIdentifier();
            break align;
        }
        current = reader.read();
        if (current != 'e') {
            reportCharacterExpectedError( 'e',current );
            skipIdentifier();
            break align;
        }
        current = reader.read();
        skipSpaces();
        preserveAspectRatioHandler.none();
        break;
    case 'x':
        current = reader.read();
        if (current != 'M') {
            reportCharacterExpectedError( 'M',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        switch (current) {
        case 'a':
            current = reader.read();
            if (current != 'x') {
                reportCharacterExpectedError( 'x',current );
                skipIdentifier();
                break align;
            }
            current = reader.read();
            if (current != 'Y') {
                reportCharacterExpectedError( 'Y',current );
                skipIdentifier();
                break align;
            }
            current = reader.read();
            if (current != 'M') {
                reportCharacterExpectedError( 'M',current );
                skipIdentifier();
                break align;
            }
            current = reader.read();
            switch (current) {
            case 'a':
                current = reader.read();
                if (current != 'x') {
                    reportCharacterExpectedError( 'x',current );
                    skipIdentifier();
                    break align;
                }
                preserveAspectRatioHandler.xMaxYMax();
                current = reader.read();
                break;
            case 'i':
                current = reader.read();
                switch (current) {
                case 'd':
                    preserveAspectRatioHandler.xMaxYMid();
                    current = reader.read();
                    break;
                case 'n':
                    preserveAspectRatioHandler.xMaxYMin();
                    current = reader.read();
                    break;
                default:
                    reportUnexpectedCharacterError( current );
                    skipIdentifier();
                    break align;
                }
            }
            break;
        case 'i':
            current = reader.read();
            switch (current) {
            case 'd':
                current = reader.read();
                if (current != 'Y') {
                    reportCharacterExpectedError( 'Y',current );
                    skipIdentifier();
                    break align;
                }
                current = reader.read();
                if (current != 'M') {
                    reportCharacterExpectedError( 'M',current );
                    skipIdentifier();
                    break align;
                }
                current = reader.read();
                switch (current) {
                case 'a':
                    current = reader.read();
                    if (current != 'x') {
                        reportCharacterExpectedError( 'x',current );
                        skipIdentifier();
                        break align;
                    }
                    preserveAspectRatioHandler.xMidYMax();
                    current = reader.read();
                    break;
                case 'i':
                    current = reader.read();
                    switch (current) {
                    case 'd':
                        preserveAspectRatioHandler.xMidYMid();
                        current = reader.read();
                        break;
                    case 'n':
                        preserveAspectRatioHandler.xMidYMin();
                        current = reader.read();
                        break;
                    default:
                        reportUnexpectedCharacterError( current );
                        skipIdentifier();
                        break align;
                    }
                }
                break;
            case 'n':
                current = reader.read();
                if (current != 'Y') {
                    reportCharacterExpectedError( 'Y',current );
                    skipIdentifier();
                    break align;
                }
                current = reader.read();
                if (current != 'M') {
                    reportCharacterExpectedError( 'M',current );
                    skipIdentifier();
                    break align;
                }
                current = reader.read();
                switch (current) {
                case 'a':
                    current = reader.read();
                    if (current != 'x') {
                        reportCharacterExpectedError( 'x',current );
                        skipIdentifier();
                        break align;
                    }
                    preserveAspectRatioHandler.xMinYMax();
                    current = reader.read();
                    break;
                case 'i':
                    current = reader.read();
                    switch (current) {
                    case 'd':
                        preserveAspectRatioHandler.xMinYMid();
                        current = reader.read();
                        break;
                    case 'n':
                        preserveAspectRatioHandler.xMinYMin();
                        current = reader.read();
                        break;
                    default:
                        reportUnexpectedCharacterError( current );
                        skipIdentifier();
                        break align;
                    }
                }
                break;
            default:
                reportUnexpectedCharacterError( current );
                skipIdentifier();
                break align;
            }
            break;
        default:
            reportUnexpectedCharacterError( current );
            skipIdentifier();
        }
        break;
    default:
        if (current != -1) {
            reportUnexpectedCharacterError( current );
            skipIdentifier();
        }
    }
    skipCommaSpaces();
    switch (current) {
    case 'm':
        current = reader.read();
        if (current != 'e') {
            reportCharacterExpectedError( 'e',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        if (current != 'e') {
            reportCharacterExpectedError( 'e',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        if (current != 't') {
            reportCharacterExpectedError( 't',current );
            skipIdentifier();
            break;
        }
        preserveAspectRatioHandler.meet();
        current = reader.read();
        break;
    case 's':
        current = reader.read();
        if (current != 'l') {
            reportCharacterExpectedError( 'l',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        if (current != 'i') {
            reportCharacterExpectedError( 'i',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        if (current != 'c') {
            reportCharacterExpectedError( 'c',current );
            skipIdentifier();
            break;
        }
        current = reader.read();
        if (current != 'e') {
            reportCharacterExpectedError( 'e',current );
            skipIdentifier();
            break;
        }
        preserveAspectRatioHandler.slice();
        current = reader.read();
        break;
    default:
        if (current != -1) {
            reportUnexpectedCharacterError( current );
            skipIdentifier();
        }
    }
    skipSpaces();
    if (current != -1) {
        reportError("end.of.stream.expected",
        new Object[] { new Integer(current) });
    }
    preserveAspectRatioHandler.endPreserveAspectRatio();
}