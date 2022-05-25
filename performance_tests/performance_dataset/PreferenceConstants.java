public static void initializeDefaultValues(IPreferenceStore store) {
    store.setDefault(PreferenceConstants.EDITOR_SHOW_SEGMENTS, false);
    // JavaBasePreferencePage
    store.setDefault(PreferenceConstants.OPEN_TYPE_HIERARCHY, PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART);
    store.setDefault(PreferenceConstants.DOUBLE_CLICK, PreferenceConstants.DOUBLE_CLICK_EXPANDS);
    store.setDefault(PreferenceConstants.UPDATE_JAVA_VIEWS, PreferenceConstants.UPDATE_WHILE_EDITING);
    store.setToDefault(PreferenceConstants.UPDATE_JAVA_VIEWS); // clear preference, update on save not supported anymore
    store.setDefault(PreferenceConstants.LINK_BROWSING_PROJECTS_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_PACKAGES_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_TYPES_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_MEMBERS_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.SEARCH_USE_REDUCED_MENU, true);
    // AppearancePreferencePage
    store.setDefault(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
    store.setDefault(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE, true);
    store.setDefault(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS, true);
    store.setDefault(PreferenceConstants.APPEARANCE_CATEGORY, true);
    store.setDefault(PreferenceConstants.SHOW_CU_CHILDREN, true);
    store.setDefault(PreferenceConstants.BROWSING_STACK_VERTICALLY, false);
    store.setDefault(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, ""); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, true);
    store.setDefault(PreferenceConstants.APPEARANCE_ABBREVIATE_PACKAGE_NAMES, false);
    store.setDefault(PreferenceConstants.APPEARANCE_PKG_NAME_ABBREVIATION_PATTERN_FOR_PKG_VIEW, ""); //$NON-NLS-1$
    // ImportOrganizePreferencePage
    store.setDefault(PreferenceConstants.ORGIMPORTS_IMPORTORDER, "java;javax;org;com"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, 99);
    store.setDefault(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, 99);
    store.setDefault(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE, true);
    // TypeFilterPreferencePage
    store.setDefault(PreferenceConstants.TYPEFILTER_ENABLED, ""); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.TYPEFILTER_DISABLED, ""); //$NON-NLS-1$
    // ClasspathVariablesPreferencePage
    // CodeFormatterPreferencePage
    // CompilerPreferencePage
    // no initialization needed
    // RefactoringPreferencePage
    store.setDefault(PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD, PreferenceConstants.REFACTOR_WARNING_SEVERITY);
    store.setDefault(PreferenceConstants.REFACTOR_SAVE_ALL_EDITORS, false);
    store.setDefault(PreferenceConstants.REFACTOR_LIGHTWEIGHT, true);
    // TemplatePreferencePage
    store.setDefault(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER, true);
    // CodeGenerationPreferencePage
    // compatibility code
    if (store.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX)) {
        String prefix= store.getString(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX);
        if (prefix.length() > 0) {
            InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID).put(JavaCore.CODEASSIST_FIELD_PREFIXES, prefix);
            store.setToDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX);
            store.setToDefault(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX);
        }
    }
    if (store.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX)) {
        String suffix= store.getString(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX);
        if (suffix.length() > 0) {
            InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID).put(JavaCore.CODEASSIST_FIELD_SUFFIXES, suffix);
            store.setToDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX);
            store.setToDefault(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX);
        }
    }
    store.setDefault(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
    store.setDefault(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
    store.setDefault(PreferenceConstants.CODEGEN_EXCEPTION_VAR_NAME, "e"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
    store.setDefault(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, true);
    // MembersOrderPreferencePage
    store.setDefault(PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER, "T,SF,SI,SM,F,I,C,M"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER, "B,V,R,D"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);
    // JavaEditorPreferencePage
    store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
    store.setDefault(PreferenceConstants.EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, false);
    store.setDefault(PreferenceConstants.EDITOR_ENCLOSING_BRACKETS, false);
    store.setDefault(PreferenceConstants.EDITOR_CORRECTION_INDICATION, true);
    store.setDefault(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, true);
    store.setDefault(PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, true);
    PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_LINKED_POSITION_COLOR, new RGB(121, 121, 121));
    store.setDefault(PreferenceConstants.EDITOR_TAB_WIDTH, 4);
    store.setDefault(PreferenceConstants.EDITOR_SPACES_FOR_TABS, false);
    store.setDefault(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_ITALIC, false);
    PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVA_ANNOTATION_COLOR, new RGB(100, 100, 100));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_ANNOTATION_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_ANNOTATION_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_STRING_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_STRING_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_DEFAULT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_DEFAULT_ITALIC, false);
    PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVA_METHOD_NAME_COLOR, new RGB(0, 0, 0));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_METHOD_NAME_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_METHOD_NAME_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_OPERATOR_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_OPERATOR_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_BRACKET_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_BRACKET_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_TASK_TAG_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_TASK_TAG_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_KEYWORD_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_KEYWORD_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_TAG_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_LINKS_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_LINKS_ITALIC, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_DEFAULT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_DEFAULT_ITALIC, false);
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION, true);
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 200);
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
    // Set the value for the deprecated color constants
    initializeDeprecatedColorConstants(store);
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, "."); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, "@#"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, true);
    store.setDefault(PreferenceConstants.CODEASSIST_CASE_SENSITIVITY, false);
    store.setDefault(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
    store.setDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, true);
    store.setDefault(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
    store.setDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);
    store.setDefault(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, false);
    store.setDefault(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES, "org.eclipse.jdt.ui.textProposalCategory\0org.eclipse.jdt.ui.javaTypeProposalCategory\0org.eclipse.jdt.ui.javaNoTypeProposalCategory\0"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_CATEGORY_ORDER, "org.eclipse.jdt.ui.spellingProposalCategory:65545\0org.eclipse.jdt.ui.javaTypeProposalCategory:65540\0org.eclipse.jdt.ui.javaNoTypeProposalCategory:65539\0org.eclipse.jdt.ui.textProposalCategory:65541\0org.eclipse.jdt.ui.javaAllProposalCategory:65542\0org.eclipse.jdt.ui.templateProposalCategory:2\0org.eclipse.jdt.ui.swtProposalCategory:3\0"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_LRU_HISTORY, ""); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_SORTER, "org.eclipse.jdt.ui.RelevanceSorter"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, ""); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, true);
    store.setDefault(PreferenceConstants.EDITOR_SMART_PASTE, true);
    store.setDefault(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_STRINGS, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_BRACES, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_JAVADOCS, true);
    store.setDefault(PreferenceConstants.EDITOR_WRAP_STRINGS, true);
    store.setDefault(PreferenceConstants.EDITOR_ESCAPE_STRINGS, false);
    store.setDefault(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, true);
    store.setDefault(PreferenceConstants.EDITOR_FORMAT_JAVADOCS, false);
    store.setDefault(PreferenceConstants.EDITOR_SMART_INDENT_AFTER_NEWLINE, true);
    int sourceHoverModifier= SWT.MOD2;
    String sourceHoverModifierName= Action.findModifierString(sourceHoverModifier);	// Shift
    int nlsHoverModifier= SWT.MOD1 + SWT.MOD3;
    String nlsHoverModifierName= Action.findModifierString(SWT.MOD1) + "+" + Action.findModifierString(SWT.MOD3);	// Ctrl + Alt //$NON-NLS-1$
    int javadocHoverModifier= SWT.MOD1 + SWT.MOD2;
    String javadocHoverModifierName= Action.findModifierString(SWT.MOD1) + "+" + Action.findModifierString(SWT.MOD2); // Ctrl + Shift //$NON-NLS-1$
    store.setDefault(PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS, "org.eclipse.jdt.ui.BestMatchHover;0;org.eclipse.jdt.ui.JavaSourceHover;" + sourceHoverModifierName + ";org.eclipse.jdt.ui.NLSStringHover;" + nlsHoverModifierName + ";org.eclipse.jdt.ui.JavadocHover;" + javadocHoverModifierName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    store.setDefault(PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIER_MASKS, "org.eclipse.jdt.ui.BestMatchHover;0;org.eclipse.jdt.ui.JavaSourceHover;" + sourceHoverModifier + ";org.eclipse.jdt.ui.NLSStringHover;" + nlsHoverModifier + ";org.eclipse.jdt.ui.JavadocHover;" + javadocHoverModifier); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    store.setDefault(PreferenceConstants.EDITOR_SMART_TAB, true);
    store.setDefault(PreferenceConstants.EDITOR_SMART_BACKSPACE, true);
    store.setDefault(PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER, false);
    store.setDefault(EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT, true);
    store.setDefault(PreferenceConstants.FORMATTER_PROFILE, FormatterProfileManager.DEFAULT_PROFILE);
    // mark occurrences
    store.setDefault(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_STICKY_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, true);
    // spell checking
    store.setDefault(PreferenceConstants.SPELLING_LOCALE, "en_US"); //$NON-NLS-1$
    String isInitializedKey= "spelling_locale_initialized"; //$NON-NLS-1$
    if (!store.getBoolean(isInitializedKey)) {
        store.setValue(isInitializedKey, true);
        Locale locale= SpellCheckEngine.getDefaultLocale();
        locale= SpellCheckEngine.findClosestLocale(locale);
        if (locale != null)
            store.setValue(PreferenceConstants.SPELLING_LOCALE, locale.toString());
    }
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_DIGITS, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_MIXED, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_SENTENCE, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_UPPER, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_URLS, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_SINGLE_LETTERS, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_AMPERSAND_IN_PROPERTIES, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_NON_LETTERS, true);
    store.setDefault(PreferenceConstants.SPELLING_IGNORE_JAVA_STRINGS, true);
    store.setDefault(PreferenceConstants.SPELLING_USER_DICTIONARY, ""); //$NON-NLS-1$
    // Note: For backwards compatibility we must use the property and not the workspace default
    store.setDefault(PreferenceConstants.SPELLING_USER_DICTIONARY_ENCODING, System.getProperty("file.encoding")); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.SPELLING_PROPOSAL_THRESHOLD, 20);
    store.setDefault(PreferenceConstants.SPELLING_PROBLEMS_THRESHOLD, 100);
    /*
    * XXX: This is currently disabled because the spelling engine
    * cannot return word proposals but only correction proposals.
    */
    store.setToDefault(PreferenceConstants.SPELLING_ENABLE_CONTENTASSIST);
    // folding
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_ENABLED, true);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_PROVIDER, "org.eclipse.jdt.ui.text.defaultFoldingProvider"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_JAVADOC, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_INNERTYPES, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_METHODS, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_IMPORTS, true);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_HEADERS, true);
    // properties file editor
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY_ITALIC, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE_ITALIC, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT_ITALIC, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT_BOLD, true);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT_ITALIC, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT_ITALIC, false);
    // semantic highlighting
    SemanticHighlightings.initDefaults(store);
    // do more complicated stuff
    NewJavaProjectPreferencePage.initDefaults(store);
    // reset preferences that are not settable by editor any longer
    // see AbstractDecoratedTextEditorPreferenceConstants
    store.setToDefault(EDITOR_SMART_HOME_END); // global
    store.setToDefault(EDITOR_LINE_NUMBER_RULER); // global
    store.setToDefault(EDITOR_LINE_NUMBER_RULER_COLOR); // global
    store.setToDefault(EDITOR_OVERVIEW_RULER); // removed -> true
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS); // accessibility
    store.setToDefault(PreferenceConstants.EDITOR_CURRENT_LINE); // global
    store.setToDefault(PreferenceConstants.EDITOR_CURRENT_LINE_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN); // global
    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN); // global
    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_FOREGROUND_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_BACKGROUND_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_FIND_SCOPE_COLOR); // global
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR); // global
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE); // global
    store.setToDefault(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED); // removed
    store.setToDefault(PreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE); // global
    //Code Clean Up
    CleanUpConstants.initDefaults(store);
    // Colors that are set by the current theme
    JavaUIPreferenceInitializer.setThemeBasedPreferences(store, false);
    store.setDefault(PREF_ANONYMOUS_EXPAND_WITH_CONSTRUCTORS, true);
    store.setDefault(PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS_MEMBERS, "java.lang.Runnable.run;java.util.concurrent.Callable.call;org.eclipse.swt.widgets.Listener.handleEvent"); //$NON-NLS-1$
    // compatibility code
    String str= store.getString(CallHierarchyContentProvider.OLD_PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS);
    if (str.length() > 0) {
        String[] oldPrefStr= str.split(";"); //$NON-NLS-1$
        for (int i= 0; i < oldPrefStr.length; i++) {
            oldPrefStr[i]= oldPrefStr[i] + (".*"); //$NON-NLS-1$
        }
        store.setValue(PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS_MEMBERS, ExpandWithConstructorsConfigurationBlock.serializeMembers(Arrays.asList(oldPrefStr)));
        store.setToDefault(CallHierarchyContentProvider.OLD_PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS);
    }
}