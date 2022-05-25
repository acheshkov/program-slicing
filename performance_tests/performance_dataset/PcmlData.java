protected void checkAttributes() {
    //String resolvedName = null;
    PcmlDocNode resolvedNode;
    super.checkAttributes();
    // Verify the count= attribute
    // If an integer was specified for the count, no checking is needed.
    // If a document element ID was was specified, make sure
    // it resolves to a <data> element with type="int".
    if (m_CountId != null) {
        resolvedNode = resolveRelativeNode(m_CountId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("count", m_CountId), getNameForException()} );
        } else {
            if (resolvedNode instanceof PcmlData) {
                if ( ((PcmlData)resolvedNode).getDataType() != PcmlData.INT ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_NODETYPE, new Object[] {makeQuotedAttr("count", m_CountId), resolvedNode.getQualifiedName(), "<data>", getNameForException()} );
                }
            } else {
                getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_DATATYPE, new Object[] {makeQuotedAttr("count", m_CountId), resolvedNode.getQualifiedName(), "type=\"int\"", getNameForException()} );
            }
        }
    } else
        // Do not allow count= to be a literal value that is negative
        if (m_Count < 0) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("count", m_Count), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    // Verify the ccsid= attribute
    // If an integer was specified for the ccsid, no checking is needed.
    // If a document element ID was was specified, make sure
    // it resolves to a <data> element with type="int".
    if (m_IsRfml && m_CcsidWasSpecified && (getDataType() != CHAR)) { // @D0A
        getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("ccsid",  getAttributeValue("ccsid")), makeQuotedAttr("type", getDataTypeString()), getBracketedTagName(), getNameForException()} );
    }
    if (m_CcsidId != null) {
        resolvedNode = resolveRelativeNode(m_CcsidId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("ccsid", m_CcsidId), getNameForException()} );
        } else {
            if (resolvedNode instanceof PcmlData) {
                if ( ((PcmlData)resolvedNode).getDataType() != PcmlData.INT ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_NODETYPE, new Object[] {makeQuotedAttr("ccsid", m_CcsidId), resolvedNode.getQualifiedName(), "<data>", getNameForException()} );
                }
            } else {
                getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_DATATYPE, new Object[] {makeQuotedAttr("ccsid", m_CcsidId), resolvedNode.getQualifiedName(), "type=\"int\"", getNameForException()} );
            }
        }
    } else
        // Do not allow ccsid= to be a literal value that is negative or greater than 65535.   @D0C
        if (m_Ccsid < 0 || m_Ccsid > 65535) { // @D0C - added check for >65535.
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("ccsid", m_Ccsid), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    // Verify the init= attribute
    if (getInit() != null) {
        try {
            PcmlDataValues.convertValue((Object) getInit(), getDataType(), getLength(), getPrecision(), getNameForException());
        } catch (Exception e) {
            getDoc().addPcmlSpecificationError(DAMRI.INITIAL_VALUE_ERROR, new Object[] {getInit(), getBracketedTagName(), getNameForException()} );
        }
    }
    // Verify the length= attribute
    // If an integer was specified for the length, no checking is needed.
    // If a document element ID was was specified, make sure
    // it resolves to a <data> element with type="int".
    if (m_LengthId != null) {
        switch (getDataType()) {
        case CHAR:
        case BYTE:
            break;
        default:
            getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), getDataTypeString(), getBracketedTagName(), getNameForException()} );
        }
        resolvedNode = resolveRelativeNode(m_LengthId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("length", m_LengthId), getNameForException()} );
        } else {
            if (resolvedNode instanceof PcmlData) {
                if ( ((PcmlData)resolvedNode).getDataType() != PcmlData.INT ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_NODETYPE, new Object[] {makeQuotedAttr("length", m_LengthId), resolvedNode.getQualifiedName(), "<data>", getNameForException()} );
                }
            } else {
                getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_DATATYPE, new Object[] {makeQuotedAttr("length", m_LengthId), resolvedNode.getQualifiedName(), "type=\"int\"", getNameForException()} );
            }
        }
    } else {
        // Verify the integer literal specified for length.
        if (m_Length == -1) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_SYNTAX, new Object[] {makeQuotedAttr("length", getAttributeValue("length")), "type=\"int\"", getBracketedTagName(), getNameForException()} );
        } else {
            switch (getDataType()) {
            case CHAR:
            case BYTE:
                if ( m_Length < 0 || m_Length > (MAX_STRING_LENGTH) ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"int\"", getBracketedTagName(), getNameForException()} );
                }
                break;
            case INT:
                if (m_Length != 2 && m_Length != 4 && m_Length != 8) {  // @C4C
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"int\"", getBracketedTagName(), getNameForException()} );
                }
                break;
            case PACKED:
                if (m_Length < 1 || m_Length > 31) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"int\"packed", getBracketedTagName(), getNameForException()} );
                }
                break;
            case ZONED:
                if (m_Length < 1 || m_Length > 31) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"int\"zoned", getBracketedTagName(), getNameForException()} );
                }
                break;
            case FLOAT:
                if (m_Length != 4 && m_Length != 8) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"float\"", getBracketedTagName(), getNameForException()} );
                }
                break;
            case STRUCT:
                if ( getAttributeValue("length") != null
                        && !getAttributeValue("length").equals("") ) { // @++C
                    getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("length",  getAttributeValue("length")), "type=\"struct\"", getBracketedTagName(), getNameForException()} );
                }
                break;
            }
            // Extra logic for RFML.                                      @D0A
            if (m_IsRfml) {
                // If type="struct", the 'struct' attribute is required.
                if (getDataType() == STRUCT) {
                    if (getAttributeValue("struct") == null ||
                            getAttributeValue("struct").equals("")) {
                        getDoc().addPcmlSpecificationError(DAMRI.NO_STRUCT, new Object[] {makeQuotedAttr("struct", null), getBracketedTagName(), getNameForException()} );
                    }
                }
                // Otherwise, the 'length' attribute is required.
                else if (!m_LengthWasSpecified) {
                    getDoc().addPcmlSpecificationError(DAMRI.NO_LENGTH, new Object[] {makeQuotedAttr("length", null), getBracketedTagName(), getNameForException()} );
                }
            }
        }
    }
    // Verify the offset= attribute
    // If an integer was specified for the offset, no checking is needed.
    // If a document element ID was was specified, make sure
    // it resolves to a <data> element with type="int".
    if (m_OffsetId != null) {
        resolvedNode = resolveRelativeNode(m_OffsetId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("offset", m_OffsetId), getNameForException()} );
        } else {
            if (resolvedNode instanceof PcmlData) {
                if ( ((PcmlData)resolvedNode).getDataType() != PcmlData.INT ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_NODETYPE, new Object[] {makeQuotedAttr("offset", m_OffsetId), resolvedNode.getQualifiedName(), "<data>", getNameForException()} );
                }
            } else {
                getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_DATATYPE, new Object[] {makeQuotedAttr("offset", m_OffsetId), resolvedNode.getQualifiedName(), "type=\"int\"", getNameForException()} );
            }
        }
    } else
        // Do not allow offset= to be a literal value that is negative
        if (m_Offset < 0) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("offset", m_Offset), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    // Verify the offsetfrom= attribute
    // If a document element ID was was specified, make sure
    // it resolves to a document element that is an ancestor of this element.
    if (m_OffsetfromId != null) {
        resolvedNode = resolveRelativeNode(m_OffsetfromId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("offsetfrom", m_OffsetfromId), getNameForException()} );
        } else {
            String qName = getQualifiedName();
            if (qName.equals("")) {
                qName = getNameForException();
            }
            String qNameResolved = resolvedNode.getQualifiedName();
            if (!qName.startsWith(qNameResolved + ".")) {
                getDoc().addPcmlSpecificationError(DAMRI.OFFSETFROM_NOT_FOUND, new Object[] {m_OffsetfromId, getNameForException()} );
            }
        }
    } else
        // Do not allow offsetfrom= to be a literal value that is negative
        if (m_Offsetfrom < -1) {                                    // @A1A
            // @A1A
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("offsetfrom", m_Offsetfrom), getBracketedTagName(), getNameForException()} ); // @A1A
        }                                                           // @A1A
    // Verify the outputsize= attribute
    // If an integer was specified for the offset, make sure it is in valid range.
    // If a document element ID was was specified, make sure
    // it resolves to a <data> element with type="int".
    if (m_OutputsizeId != null) {
        resolvedNode = resolveRelativeNode(m_OutputsizeId);
        if (resolvedNode == null) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_NOT_FOUND, new Object[] {makeQuotedAttr("outputsize", m_OutputsizeId), getNameForException()} );
        } else {
            if (resolvedNode instanceof PcmlData) {
                if ( ((PcmlData)resolvedNode).getDataType() != PcmlData.INT ) {
                    getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_NODETYPE, new Object[] {makeQuotedAttr("outputsize", m_OutputsizeId), resolvedNode.getQualifiedName(), "<data>", getNameForException()} );
                }
            } else {
                getDoc().addPcmlSpecificationError(DAMRI.ATTR_REF_WRONG_DATATYPE, new Object[] {makeQuotedAttr("outputsize", m_OutputsizeId), resolvedNode.getQualifiedName(), "type=\"int\"", getNameForException()} );
            }
        }
    } else
        // Do not allow offset= to be a literal value that is negative
        if (m_Outputsize < 0) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("outputsize", m_Outputsize), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    // Verify the precision= attribute
    if (getAttributeValue("precision") != null
            &&  !getAttributeValue("precision").equals("") ) { // @++C
        switch (getDataType()) {
        // precision= is not allowed for these data types
        case CHAR:
        case BYTE:
        case FLOAT:
        case STRUCT:
            getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("precision",  getAttributeValue("precision")), makeQuotedAttr("type", getDataTypeString()), getBracketedTagName(), getNameForException()} );
            break;
        // For type=int, precision= must be 15 or 16 or 21 or 32 depending on length=
        case INT:
            if (m_Length == 2) {
                if (m_Precision != 15 && m_Precision != 16) {
                    getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("precision",  getAttributeValue("precision")), getBracketedTagName(), getNameForException()} );
                }
            }
            if (m_Length == 4) {
                if (m_Precision != 31 && m_Precision != 32) {
                    getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("precision",  getAttributeValue("precision")), getBracketedTagName(), getNameForException()} );
                }
            }
            if (m_Length == 8) {                            // @C4A
                // @C4A
                if (m_Precision != 63) {                    // @C4A
                    // @C4A
                    getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("precision",  getAttributeValue("precision")), getBracketedTagName(), getNameForException()} ); // @C4A
                }                                           // @C4A
            }                                               // @C4A
            break;
        // For type=packed and type=zoned,
        // precision= must be >= 0 and <= the data length (length=)
        case PACKED:
        case ZONED:
            if (m_Precision < 0 || m_Precision > m_Length) {
                getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("precision",  getAttributeValue("precision")), getBracketedTagName(), getNameForException()} );
            }
            break;
        }
    }
    // Verify the struct= attribute
    if (m_StructId != null) {
        if (getDataType() != STRUCT) {
            getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("struct",  getAttributeValue("struct")), makeQuotedAttr("type", getDataTypeString()), getBracketedTagName(), getNameForException()} );
        }
    }
    // Verify the minvrm= attribute
    if (m_Minvrm != null) {
        m_MinvrmInt = validateVRM(m_Minvrm);
        if (m_MinvrmInt <= 0) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("minvrm", m_Minvrm), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    }
    // Verify the maxvrm= attribute
    if (m_Maxvrm != null) {
        m_MaxvrmInt = validateVRM(m_Maxvrm);
        if (m_MaxvrmInt <= 0) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_ATTRIBUTE_VALUE, new Object[] {makeQuotedAttr("maxvrm", m_Maxvrm), getBracketedTagName(), getNameForException()} ); // @A1C
        }
    }
    // Verify the passby= attribute
    if (m_PassbyStr != null) {                                  // @B1A
        // @B1A
        // Only allow this attribute when the pcml version is 2.0 or higher (e.g. <pcml version="2.0">)
        if ( getDoc().getVersion().compareTo("2.0") < 0 ) {     // @B1A
            // @B1A
            getDoc().addPcmlSpecificationError(DAMRI.BAD_PCML_VERSION, new Object[] {makeQuotedAttr("passby", m_PassbyStr), "2.0", getBracketedTagName(), getNameForException()} ); // @B1A @C9C
        }                                                       // @B1A
        // Only allow this attribute when it is a child of <program>
        if ( !(getParent() instanceof PcmlProgram) ) {          // @B1A
            // @B1A
            getDoc().addPcmlSpecificationError(DAMRI.NOT_CHILD_OF_PGM, new Object[] {makeQuotedAttr("passby", m_PassbyStr), getBracketedTagName(), getNameForException()} ); // @B1A
        }                                                       // @B1A
    }                                                           // @B1A
    // Verify the bidistringtype= attribute
    if (m_BidistringtypeStr != null) {                          // @C9A
        // @C9A
        // Only allow this attribute when the pcml version is 3.0 or higher (e.g. <pcml version="3.0">)
        if ( getDoc().getVersion().compareTo("3.0") < 0 ) {     // @C9A
            // @C9A
            getDoc().addPcmlSpecificationError(DAMRI.BAD_PCML_VERSION, new Object[] {makeQuotedAttr("bidistringtype", m_BidistringtypeStr), "3.0", getBracketedTagName(), getNameForException()} ); // @C9A
        }                                                       // @C9A
    }
    // Verify the trim= attribute
    if (m_TrimStr != null) {                                    // @D1A
        // @D1A
        // Only allow this attribute when the pcml version is 4.0 or higher (e.g. <pcml version="4.0">)
        if ( getDoc().getVersion().compareTo("4.0") < 0 ) {     // @D1A
            // @D1A
            getDoc().addPcmlSpecificationError(DAMRI.BAD_PCML_VERSION, new Object[] {makeQuotedAttr("trim", m_TrimStr), "4.0", getBracketedTagName(), getNameForException()} ); // @D1A
        }                                                       // @D1A
    }
    // Verify the chartype= attribute
    if (m_CharType != null) {                                   // @D2A
        // @D2A
        // Only allow this attribute when the pcml version is 4.0 or higher (e.g. <pcml version="4.0">)
        if ( getDoc().getVersion().compareTo("4.0") < 0 ) {     // @D2A
            // @D2A
            getDoc().addPcmlSpecificationError(DAMRI.BAD_PCML_VERSION, new Object[] {makeQuotedAttr("chartype", m_CharType), "4.0", getBracketedTagName(), getNameForException()} ); // @D2A
        }                                                       // @D2A
        else {
            if (getDataType() != CHAR) {                           // @D2A
                getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("chartype",  getAttributeValue("chartype")), makeQuotedAttr("type", getDataTypeString()), getBracketedTagName(), getNameForException()} );
            }
        }
    }
    // Verify the keyfield= attribute  (applies only to RFML)
    if (m_KeyFieldStr != null) {
        // Only allow this attribute when the rfml version is 5.0 or higher (e.g. <rfml version="5.0">)
        if (!m_IsRfml) { // if not rfml, then assume it's pcml
            getDoc().addPcmlSpecificationError(DAMRI.ATTRIBUTE_NOT_ALLOWED, new Object[] {makeQuotedAttr("keyfield",  getAttributeValue("keyfield")), makeQuotedAttr("pcml", getDataTypeString()), getBracketedTagName(), getNameForException()} );
        }
        if ( getDoc().getVersion().compareTo("5.0") < 0 ) {
            getDoc().addPcmlSpecificationError(DAMRI.BAD_PCML_VERSION, new Object[] {makeQuotedAttr("keyfield", m_KeyFieldStr), "5.0", getBracketedTagName(), getNameForException()} );
        }
    }
}