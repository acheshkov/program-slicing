public void loadUserInformation() throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
    if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Loading user information.");
    if (system_ == null) {
        Trace.log(Trace.ERROR, "Cannot connect to server before setting system.");
        throw new ExtendedIllegalStateException("system", ExtendedIllegalStateException.PROPERTY_NOT_SET);
    }
    if (name_ == null) {
        Trace.log(Trace.ERROR, "Cannot connect to server before setting name.");
        throw new ExtendedIllegalStateException("name", ExtendedIllegalStateException.PROPERTY_NOT_SET);
    }
    Converter conv = new Converter(system_.getCcsid(), system_);
    byte[] userProfileName = new byte[] { 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40 };
    conv.stringToByteArray(name_, userProfileName);
    int receiverVariableLength = 1024;
    ProgramParameter[] parameters = new ProgramParameter[] {
        // Receiver variable, output, char(*).
        new ProgramParameter(receiverVariableLength),
        // Receiver variable length, input, binary(4).
        new ProgramParameter(BinaryConverter.intToByteArray(receiverVariableLength)),
        // Format name, input, char(8), EBCDIC 'USRI0300'.
        new ProgramParameter(new byte[] { (byte)0xE4, (byte)0xE2, (byte)0xD9, (byte)0xC9, (byte)0xF0, (byte)0xF3, (byte)0xF0, (byte)0xF0 } ),
        // User profile name, input, char(10).
        new ProgramParameter(userProfileName),
        // Error code, I/O, char(*).
        ERROR_CODE
    };
    ProgramCall pc = new ProgramCall(system_, "/QSYS.LIB/QSYRUSRI.PGM", parameters);
    // Note: QSYRUSRI is designated "Threadsafe: Yes".
    // But honor the ProgramCall.threadsafe property if set.
    pc.suggestThreadsafe();
    if (!pc.run()) {
        throw new AS400Exception(pc.getMessageList());
    }
    byte[] data = parameters[0].getOutputData();
    int bytesReturned = BinaryConverter.byteArrayToInt(data, 0);
    int bytesAvailable = BinaryConverter.byteArrayToInt(data, 4);
    if (bytesReturned < bytesAvailable) {
        if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Retrieve user information receiver variable too small, bytes returned: " + bytesReturned + ", bytes available: " + bytesAvailable);
        receiverVariableLength = bytesAvailable;
        try {
            parameters[0].setOutputDataLength(receiverVariableLength);
            parameters[1].setInputData(BinaryConverter.intToByteArray(receiverVariableLength));
        } catch (PropertyVetoException e) {
            Trace.log(Trace.ERROR, "Unexpected PropertyVetoException:", e);
            throw new InternalErrorException(InternalErrorException.UNEXPECTED_EXCEPTION, e.getMessage());
        }
        if (!pc.run()) {
            throw new AS400Exception(pc.getMessageList());
        }
        data = parameters[0].getOutputData();
    }
    userProfileName_ = conv.byteArrayToString(data, 8, 10).trim();
    // Previous sign-on is in format:  "CYYMMDDHHMMSS".
    String previousSignon = conv.byteArrayToString(data, 18, 13); // Note: This time value is relative to the system's local time zone, not UTC.
    if (previousSignon.trim().length() > 0) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 1900 + Integer.parseInt(previousSignon.substring(0, 3)));
        cal.set(Calendar.MONTH, Integer.parseInt(previousSignon.substring(3, 5)) - 1);
        cal.set(Calendar.DATE, Integer.parseInt(previousSignon.substring(5, 7)));
        cal.set(Calendar.HOUR, Integer.parseInt(previousSignon.substring(7, 9)));
        cal.set(Calendar.MINUTE, Integer.parseInt(previousSignon.substring(9, 11)));
        cal.set(Calendar.SECOND, Integer.parseInt(previousSignon.substring(11, 13)));
        // Set the correct time zone (in case client is in a different zone than server).
        if (systemTimeZone_ == null) {
            try {
                systemTimeZone_ = getDateTimeConverter().getSystemTimeZone();
            } catch (Throwable t) {
                if (Trace.traceOn_) Trace.log(Trace.WARNING, "Unable to determine time zone of system. Assuming server is in the same time zone as client application.", t);
            }
        }
        if (systemTimeZone_ != null) cal.setTimeZone(systemTimeZone_);
        previousSignedOnDate_ = cal.getTime();
    } else {
        previousSignedOnDate_ = null;
    }
    signedOnAttemptsNotValid_ = BinaryConverter.byteArrayToInt(data, 32);
    status_ = conv.byteArrayToString(data, 36, 10).trim();
    if (passwordLastChangedDateBytes_ == null) passwordLastChangedDateBytes_ = new byte[8];
    // *DTS format - convert on getter.
    System.arraycopy(data, 46, passwordLastChangedDateBytes_, 0, 8);
    passwordLastChangedDate_ = null;  // Reset.
    // EBCDIC 'Y' for no password.
    noPassword_ = (data[54] == (byte)0xE8);
    // 1-366.  0 means use system value QPWDEXPITV.  -1 means *NOMAX.
    passwordExpirationInterval_ = BinaryConverter.byteArrayToInt(data, 56);
    if (passwordExpireDateBytes_ == null) passwordExpireDateBytes_ = new byte[8];
    // *DTS format.
    System.arraycopy(data, 60, passwordExpireDateBytes_, 0, 8);
    passwordExpireDate_ = null;  // Reset.
    daysUntilPasswordExpire_ = BinaryConverter.byteArrayToInt(data, 68);
    // EBCDIC 'Y' if the user's password is set to expired.
    passwordSetExpire_ = (data[72] == (byte)0xE8);
    userClassName_ = conv.byteArrayToString(data, 73, 10).trim();
    int numSpecAuth = 0;
    for (int i = 0; i < 8; ++i) {
        if (data[83 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            ++numSpecAuth;
        }
    }
    specialAuthority_ = new String[numSpecAuth];
    int counter = 0;
    for (int i = 0; i < 8; ++i) {
        if (data[83 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            specialAuthority_[counter++] = SPECIAL_AUTHORITIES[i];
        }
    }
    groupProfileName_ = conv.byteArrayToString(data, 98, 10).trim();
    owner_ = conv.byteArrayToString(data, 108, 10).trim();
    groupAuthority_ = conv.byteArrayToString(data, 118, 10).trim();
    assistanceLevel_ = conv.byteArrayToString(data, 128, 10).trim();
    currentLibraryName_ = conv.byteArrayToString(data, 138, 10).trim();
    String menu = conv.byteArrayToString(data, 148, 10).trim();
    initialMenu_ = menu.equals("*SIGNOFF") ? menu : QSYSObjectPathName.toPath(conv.byteArrayToString(data, 158, 10).trim(), menu, "MNU");
    String prog = conv.byteArrayToString(data, 168, 10).trim();
    initialProgram_ = prog.equals(NONE) ? NONE : QSYSObjectPathName.toPath(conv.byteArrayToString(data, 178, 10).trim(), prog, "PGM");
    limitCapabilities_ = conv.byteArrayToString(data, 188, 10).trim();
    description_ = conv.byteArrayToString(data, 198, 50).trim();
    displaySignOnInformation_ = conv.byteArrayToString(data, 248, 10).trim();
    limitDeviceSessions_ = conv.byteArrayToString(data, 258, 10).trim();
    keyboardBuffering_ = conv.byteArrayToString(data, 268, 10).trim();
    maximumStorageAllowed_ = BinaryConverter.byteArrayToInt(data, 280);
    storageUsed_ = BinaryConverter.byteArrayToInt(data, 284);
    highestSchedulingPriority_ = data[288] & 0x0000000F;
    jobDescription_ = QSYSObjectPathName.toPath(conv.byteArrayToString(data, 299, 10).trim(), conv.byteArrayToString(data, 289, 10).trim(), "JOBD");
    accountingCode_ = conv.byteArrayToString(data, 309, 15).trim();
    messageQueue_ = QSYSObjectPathName.toPath(conv.byteArrayToString(data, 334, 10).trim(), conv.byteArrayToString(data, 324, 10).trim(), "MSGQ");
    messageQueueDeliveryMethod_ = conv.byteArrayToString(data, 344, 10).trim();
    messageQueueSeverity_ = BinaryConverter.byteArrayToInt(data, 356);
    String outQueueName = conv.byteArrayToString(data, 360, 10).trim();
    outputQueue_ = outQueueName.equals("*WRKSTN") || outQueueName.equals("*DEV") ? outQueueName : QSYSObjectPathName.toPath(conv.byteArrayToString(data, 370, 10).trim(), outQueueName, "OUTQ");
    printDevice_ = conv.byteArrayToString(data, 380, 10).trim();
    specialEnvironment_ = conv.byteArrayToString(data, 390, 10).trim();
    String keyName = conv.byteArrayToString(data, 400, 10).trim();
    attentionKeyHandlingProgram_ = keyName.equals(NONE) || keyName.equals("*SYSVAL") ? keyName : QSYSObjectPathName.toPath(conv.byteArrayToString(data, 410, 10).trim(), keyName, "PGM");
    languageID_ = conv.byteArrayToString(data, 420, 10).trim();
    countryID_ = conv.byteArrayToString(data, 430, 10).trim();
    ccsid_ = BinaryConverter.byteArrayToInt(data, 440);
    int numUserOptions = 0;
    for (int i = 0; i < 7; ++i) {
        if (data[444 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            ++numUserOptions;
        }
    }
    userOptions_ = new String[numUserOptions];
    counter = 0;
    for (int i = 0; i < 7; ++i) {
        if (data[444 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            userOptions_[counter++] = USER_OPTIONS[i];
        }
    }
    String sortName = conv.byteArrayToString(data, 480, 10).trim();
    sortSequenceTable_ = sortName.equals("*HEX") || sortName.equals("*LANGIDUNQ") || sortName.equals("*LANGIDSHR") || sortName.equals("*SYSVAL") || sortName.length() == 0 ? sortName : QSYSObjectPathName.toPath(conv.byteArrayToString(data, 490, 10).trim(), sortName, "FILE");
    objectAuditingValue_ = conv.byteArrayToString(data, 500, 10).trim();
    int numAudLevel = 0;
    for (int i = 0; i < 13; ++i) {
        if (data[510 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            ++numAudLevel;
        }
    }
    userActionAuditLevel_ = new String[numAudLevel];
    counter = 0;
    for (int i = 0; i < 13; ++i) {
        if (data[510 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            userActionAuditLevel_[counter++] = AUDIT_LEVELS[i];
        }
    }
    groupAuthorityType_ = conv.byteArrayToString(data, 574, 10).trim();
    int supplementalGroupOffset = BinaryConverter.byteArrayToInt(data, 584);
    int supplementalGroupCount = BinaryConverter.byteArrayToInt(data, 588);
    supplementalGroups_ = new String[supplementalGroupCount];
    for (int i = 0; i < supplementalGroupCount; ++i) {
        supplementalGroups_[i] = conv.byteArrayToString(data, supplementalGroupOffset + i * 10, 10).trim();
    }
    userID_ = BinaryConverter.byteArrayToUnsignedInt(data, 592);
    groupID_ = BinaryConverter.byteArrayToUnsignedInt(data, 596);
    int homeDirOffset = BinaryConverter.byteArrayToInt(data, 600);
    int homeDirCcsid = BinaryConverter.byteArrayToInt(data, homeDirOffset);
    int homeDirLength = BinaryConverter.byteArrayToInt(data, homeDirOffset + 16);
    Converter homeDirConv = homeDirCcsid > 0 && homeDirCcsid < 65535 ? new Converter(homeDirCcsid, system_) : conv;
    homeDirectory_ = homeDirConv.byteArrayToString(data, homeDirOffset + 32, homeDirLength).trim();
    int numLocaleJobAttribs = 0;
    for (int i = 0; i < 8; ++i) {
        if (data[608 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            ++numLocaleJobAttribs;
        }
    }
    localeJobAttributes_ = new String[numLocaleJobAttribs];
    counter = 0;
    for (int i = 0; i < 8; ++i) {
        if (data[608 + i] == (byte)0xE8) { // EBCDIC 'Y' is 0xE8.
            localeJobAttributes_[counter++] = LOCALE_ATTRIBUTES[i];
        }
    }
    int localePathOffset = BinaryConverter.byteArrayToInt(data, 624);
    int localePathLength = BinaryConverter.byteArrayToInt(data, 628);
    if (localePathLength == 10) {
        localePathName_ = conv.byteArrayToString(data, localePathOffset, localePathLength).trim();
    } else {
        int localePathCcsid = BinaryConverter.byteArrayToInt(data, localePathOffset);
        localePathLength = BinaryConverter.byteArrayToInt(data, localePathOffset + 16);
        Converter localePathConv = localePathCcsid > 0 && localePathCcsid < 65535 ? new Converter(localePathCcsid, system_) : conv;
        localePathName_ = localePathConv.byteArrayToString(data, localePathOffset + 32, localePathLength).trim();
    }
    // EBCDIC '1' indicates the user is a group that has members.
    groupHasMember_ = (data[632] == (byte)0xF1);
    // EBCDIC '1' indicates there is at least one digital certificate associated with this user.
    withDigitalCertificates_ = (data[633] == (byte)0xF1);
    chridControl_ = conv.byteArrayToString(data, 634, 10).trim();
    int vrm = system_.getVRM();
    if (vrm >= 0x00050100) {
        int iaspOffset = BinaryConverter.byteArrayToInt(data, 644);
        int iaspCount = BinaryConverter.byteArrayToInt(data, 648);
        int iaspCountReturned = BinaryConverter.byteArrayToInt(data, 652);
        int iaspLength = BinaryConverter.byteArrayToInt(data, 656);
        if (Trace.traceOn_ && iaspCount != iaspCountReturned) {
            Trace.log(Trace.WARNING, "Not all IASP information was returned, count: " + iaspCount + ", returned:", iaspCountReturned);
        }
        iaspNames_ = new String[iaspCountReturned];
        iaspStorageAllowed_ = new int[iaspCountReturned];
        iaspStorageUsed_ = new int[iaspCountReturned];
        for (int i = 0; i < iaspCountReturned; ++i) {
            int offset = iaspOffset + (i * iaspLength);
            iaspNames_[i] = conv.byteArrayToString(data, offset, 10).trim();
            iaspStorageAllowed_[i] = BinaryConverter.byteArrayToInt(data, offset + 12);
            iaspStorageUsed_[i] = BinaryConverter.byteArrayToInt(data, offset + 16);
        }
        if (vrm >= 0x00050300) {
            // EBCDIC 'Y' indicates the password is managed locally.
            localPasswordManagement_ = (data[660] == (byte)0xE8);
            if (vrm >= 0x00060100) {	// @550 added password change block
                pwdChangeBlock_ = conv.byteArrayToString(data, 661, 10).trim();
                // EBCDIC '1' indicates user entitlement is required.
                userEntitlementRequired_ = (data[671] == (byte)0xF1);
                if (vrm >= 0x00070100) {	// @710 added more fields
                    userExpirationInterval_ = BinaryConverter.byteArrayToInt(data, 672);
                    if (userExpirationDateBytes_ == null) userExpirationDateBytes_ = new byte[8];
                    // *DTS format - convert on getter.
                    System.arraycopy(data, 676, userExpirationDateBytes_, 0, 8);
                    userExpirationDate_ = null;  // Reset.
                    userExpirationAction_ = conv.byteArrayToString(data, 684, 10).trim();
                }
            }
        }
    }
    loaded_ = true;
    connected_ = true;
}