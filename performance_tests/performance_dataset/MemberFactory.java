public FieldInfo createFieldInfo(final XMLBindingComponent component,
                                 final ClassInfoResolver resolver, final boolean useJava50) {
    String xmlName = component.getXMLName();
    String memberName = component.getJavaMemberName();
    if (!memberName.startsWith("_")) {
        memberName = "_" + memberName;
    }
    XMLType xmlType = component.getXMLType();
    ClassInfo classInfo = resolver.resolve(component);
    XSType   xsType = null;
    FieldInfo fieldInfo = null;
    boolean enumeration = false;
    boolean simpleTypeCollection = false;
    if (xmlType != null) {
        if (xmlType.isSimpleType() ) {
            SimpleType simpleType = (SimpleType) xmlType;
            SimpleType baseType = null;
            String derivationMethod = simpleType.getDerivationMethod();
            if (derivationMethod != null) {
                if (SchemaNames.RESTRICTION.equals(derivationMethod)) {
                    baseType = (SimpleType) simpleType.getBaseType();
                }
            }
            //-- handle special case for enumerated types
            if (simpleType.hasFacet(Facet.ENUMERATION)) {
                //-- LOok FoR CLasSiNfO iF ReSoLvR is NoT NuLL
                enumeration = true;
                if (resolver != null) {
                    classInfo = resolver.resolve(xmlType);
                }
                if (classInfo != null) {
                    XMLInfoNature xmlNature = new XMLInfoNature(classInfo);
                    xsType = xmlNature.getSchemaType();
                }
            } else if ((simpleType instanceof ListType) || (baseType instanceof ListType)) {
                if (baseType != null) {
                    if (!baseType.isBuiltInType()) {
                        simpleTypeCollection = true;
                    }
                } else {
                    if (!simpleType.isBuiltInType()) {
                        simpleTypeCollection = true;
                    }
                }
                // handle special case where the list type uses an item type
                // that has enumeration facets defined.
                ListType listType = (ListType) simpleType;
                if (listType == null) {
                    listType = (ListType) baseType;
                }
                SimpleType itemType = listType.getItemType();
                if (itemType.hasFacet(Facet.ENUMERATION)) {
                    ClassInfo itemClassInfo = resolver.resolve(itemType);
                    if (itemClassInfo != null) {
                        xsType = new XMLInfoNature(itemClassInfo).getSchemaType();
                    } else {
                        XMLBindingComponent temp = new XMLBindingComponent(
                            getConfig(), getGroupNaming());
                        temp.setBinding(component.getBinding());
                        temp.setView(itemType);
                        String packageName = temp.getJavaPackage();
                        if (packageName != null && packageName.length() > 0) {
                            packageName = packageName + "." + SourceGeneratorConstants.TYPES_PACKAGE;
                        } else {
                            packageName = SourceGeneratorConstants.TYPES_PACKAGE;
                        }
                        JClass tempClass = new JClass(packageName+ "." + temp.getJavaClassName());
                        xsType = new XSClass(tempClass);
                        xsType.setAsEnumerated(true);
                    }
                }
            }
            if (xsType == null) {
                xsType = component.getJavaType();
            }
        } else if (xmlType.isAnyType()) {
            //-- Just treat as java.lang.Object.
            if (classInfo != null) {
                XMLInfoNature xmlNature = new XMLInfoNature(classInfo);
                xsType = xmlNature.getSchemaType();
            }
            if (xsType == null) {
                xsType = new XSClass(SGTypes.OBJECT);
            }
        } else if (xmlType.isComplexType() && (xmlType.getName() != null)) {
            //--if we use the type method then no class is output for
            //--the element we are processing
            if (getConfig().mappingSchemaType2Java()) {
                XMLBindingComponent temp = new XMLBindingComponent(
                    getConfig(), getGroupNaming());
                temp.setBinding(component.getBinding());
                temp.setView(xmlType);
                ClassInfo typeInfo = resolver.resolve(xmlType);
                if (typeInfo != null) {
                    // if we have not processed the <complexType> referenced
                    // by the ClassInfo yet, this will return null
                    // TODO find a way to resolve an unprocessed <complexType>
                    XMLInfoNature xmlNature = new XMLInfoNature(typeInfo);
                    xsType = xmlNature.getSchemaType();
                } else {
                    String className = temp.getQualifiedName();
                    if (className != null) {
                        JClass jClass = new JClass(className);
                        if (((ComplexType) xmlType).isAbstract()) {
                            jClass.getModifiers().setAbstract(true);
                        }
                        xsType = new XSClass(jClass);
                        className = null;
                    }
                }
            }
        } // complexType
    } else {
        if (xsType == null) {
            xsType = component.getJavaType();
        }
        if (xsType == null) {
            //-- patch for bug 1471 (No XMLType specified)
            //-- treat unspecified type as anyType
            switch (component.getAnnotated().getStructureType()) {
            case Structure.ATTRIBUTE:
                AttributeDecl attribute = (AttributeDecl) component.getAnnotated();
                if (!attribute.hasXMLType()) {
                    xsType = new XSClass(SGTypes.OBJECT);
                }
                break;
            case Structure.ELEMENT:
                ElementDecl element = (ElementDecl) component.getAnnotated();
                if (!element.hasXMLType()) {
                    xsType = new XSClass(SGTypes.OBJECT);
                }
                break;
            default:
                // probably a model-group
                break;
            }
        }
    }
    // is the XSType found?
    if (xsType == null) {
        String className = component.getQualifiedName();
        JClass jClass = new JClass(className);
        if (component.isAbstract()) {
            jClass.getModifiers().setAbstract(true);
        }
        if (getConfig().isAutomaticConflictResolution()) {
            getSourceGenerator().getXMLInfoRegistry().bind(jClass,
                    component, "field");
        }
        xsType = new XSClass(jClass);
        if (xmlType != null && xmlType.isComplexType()) {
            ComplexType complexType = (ComplexType) xmlType;
            if (complexType.isAbstract() || getConfig().mappingSchemaElement2Java()) {
                jClass.getModifiers().setAbstract(true);
            }
        }
        className = null;
    }
    // create the fieldInfo
    // check whether this should be a collection or not
    int maxOccurs = component.getUpperBound();
    int minOccurs = component.getLowerBound();
    if (simpleTypeCollection
            || ((maxOccurs < 0 || maxOccurs > 1) && !this.isChoice(component))) {
        String vName = memberName + "List";
        // if xmlName is null it means that
        // we are processing a container object (group)
        // so we need to adjust the name of the members of the collection
        CollectionInfo cInfo;
        cInfo = this.getInfoFactory().createCollection(xsType, vName, memberName,
                component.getCollectionType(), getJavaNaming(), useJava50);
        XSListType xsList = cInfo.getXSList();
        if (!simpleTypeCollection) {
            xsList.setMaximumSize(maxOccurs);
            xsList.setMinimumSize(minOccurs);
        } else {
            if (xsList instanceof XSList) {
                ((XSList) xsList).setDerivedFromXSList(true);
            }
        }
        fieldInfo = cInfo;
    } else  {
        switch (xsType.getType()) {
        case XSType.ID_TYPE:
            fieldInfo = this.getInfoFactory().createIdentity(memberName);
            break;
        case XSType.COLLECTION:
        case XSType.IDREFS_TYPE:
        case XSType.NMTOKENS_TYPE:
            String collectionName = component.getCollectionType();
            XSType contentType = ((XSListType) xsType).getContentType();
            fieldInfo = this.getInfoFactory().createCollection(contentType,
                        memberName, memberName,
                        collectionName, getJavaNaming(), useJava50);
            break;
        default:
            fieldInfo = this.getInfoFactory().createFieldInfo(xsType, memberName);
            break;
        }
    }
    // initialize the field
    XMLInfoNature xmlNature = new XMLInfoNature(fieldInfo);
    xmlNature.setNodeName(xmlName);
    xmlNature.setRequired(minOccurs > 0);
    switch (component.getAnnotated().getStructureType()) {
    case Structure.ELEMENT:
        xmlNature.setNodeType(NodeType.ELEMENT);
        break;
    case Structure.ATTRIBUTE:
        xmlNature.setNodeType(NodeType.ATTRIBUTE);
        break;
    case Structure.MODELGROUP:
    case Structure.GROUP:
        xmlNature.setNodeName(XMLInfo.CHOICE_NODE_NAME_ERROR_INDICATION);
        fieldInfo.setContainer(true);
        break;
    default:
        break;
    }
    //-- handle namespace URI / prefix
    String nsURI = component.getTargetNamespace();
    if ((nsURI != null) && (nsURI.length() > 0)) {
        xmlNature.setNamespaceURI(nsURI);
        // TODO set the prefix used in the XML Schema
        //      in order to use it inside the Marshaling Framework
    }
    // handle default value (if any is set)
    handleDefaultValue(component, classInfo, xsType, fieldInfo, enumeration);
    //-- handle nillable values
    if (component.isNillable()) {
        fieldInfo.setNillable(true);
    }
    //-- add annotated comments
    String comment = createComment(component.getAnnotated());
    if (comment != null) {
        fieldInfo.setComment(comment);
    }
    //--specific field handler or validator?
    if (component.getXMLFieldHandler() != null) {
        fieldInfo.setXMLFieldHandler(component.getXMLFieldHandler());
    }
    if (component.getValidator() != null) {
        fieldInfo.setValidator(component.getValidator());
    }
    if (component.getVisiblity() != null) {
        String visibility = component.getVisiblity();
        fieldInfo.setVisibility(visibility);
    }
    // deal with substitution groups
    switch (component.getAnnotated().getStructureType()) {
    case Structure.ELEMENT:
        ElementDecl elementDeclaration = (ElementDecl) component.getAnnotated();
        if (elementDeclaration.isReference()) {
            elementDeclaration = elementDeclaration.getReference();
        }
        Enumeration<ElementDecl> possibleSubstitutes = elementDeclaration.getSubstitutionGroupMembers();
        if (possibleSubstitutes.hasMoreElements()) {
            List<String> substitutionGroupMembers = new ArrayList<String>();
            while (possibleSubstitutes.hasMoreElements()) {
                ElementDecl substitute = possibleSubstitutes.nextElement();
                substitutionGroupMembers.add(substitute.getName());
            }
            fieldInfo.setSubstitutionGroupMembers(substitutionGroupMembers);
        }
    default:
    }
    return fieldInfo;
}