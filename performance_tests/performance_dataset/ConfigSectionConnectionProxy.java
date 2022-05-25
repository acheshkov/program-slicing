public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Composite cSection = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                            | GridData.HORIZONTAL_ALIGN_FILL);
    cSection.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cSection.setLayout(layout);
    int userMode = COConfigurationManager.getIntParameter("User Mode");
    if (userMode < REQUIRED_MODE) {
        Label label = new Label(cSection, SWT.WRAP);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);
        final String[] modeKeys = { "ConfigView.section.mode.beginner",
                                    "ConfigView.section.mode.intermediate",
                                    "ConfigView.section.mode.advanced"
                                  };
        String param1, param2;
        if (REQUIRED_MODE < modeKeys.length)
            param1 = MessageText.getString(modeKeys[REQUIRED_MODE]);
        else
            param1 = String.valueOf(REQUIRED_MODE);
        if (userMode < modeKeys.length)
            param2 = MessageText.getString(modeKeys[userMode]);
        else
            param2 = String.valueOf(userMode);
        label.setText(MessageText.getString("ConfigView.notAvailableForMode",
                                            new String[] { param1, param2 } ));
        return cSection;
    }
    //////////////////////  PROXY GROUP /////////////////
    Group gProxyTracker = new Group(cSection, SWT.NULL);
    Messages.setLanguageText(gProxyTracker, CFG_PREFIX + "group.tracker");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gProxyTracker.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gProxyTracker.setLayout(layout);
    final BooleanParameter enableProxy = new BooleanParameter(gProxyTracker,
            "Enable.Proxy", CFG_PREFIX + "enable_proxy");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableProxy.setLayoutData(gridData);
    final BooleanParameter enableSocks = new BooleanParameter(gProxyTracker,
            "Enable.SOCKS", CFG_PREFIX + "enable_socks");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableSocks.setLayoutData(gridData);
    Label lHost = new Label(gProxyTracker, SWT.NULL);
    Messages.setLanguageText(lHost, CFG_PREFIX + "host");
    final StringParameter pHost = new StringParameter(gProxyTracker, "Proxy.Host", "", false );
    gridData = new GridData();
    gridData.widthHint = 105;
    pHost.setLayoutData(gridData);
    Label lPort = new Label(gProxyTracker, SWT.NULL);
    Messages.setLanguageText(lPort, CFG_PREFIX + "port");
    final StringParameter pPort = new StringParameter(gProxyTracker, "Proxy.Port", "", false );
    gridData = new GridData();
    gridData.widthHint = 40;
    pPort.setLayoutData(gridData);
    Label lUser = new Label(gProxyTracker, SWT.NULL);
    Messages.setLanguageText(lUser, CFG_PREFIX + "username");
    final StringParameter pUser = new StringParameter(gProxyTracker, "Proxy.Username", false );
    gridData = new GridData();
    gridData.widthHint = 105;
    pUser.setLayoutData(gridData);
    Label lPass = new Label(gProxyTracker, SWT.NULL);
    Messages.setLanguageText(lPass, CFG_PREFIX + "password");
    final StringParameter pPass = new StringParameter(gProxyTracker, "Proxy.Password", "", false );
    gridData = new GridData();
    gridData.widthHint = 105;
    pPass.setLayoutData(gridData);
    final NetworkAdminSocksProxy[]	test_proxy = { null };
    final Button test_socks = new Button(gProxyTracker, SWT.PUSH);
    Messages.setLanguageText(test_socks, CFG_PREFIX	+ "testsocks");
    test_socks.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
            final NetworkAdminSocksProxy target;
            synchronized( test_proxy ) {
                target = test_proxy[0];
            }
            if ( target != null ) {
                final TextViewerWindow viewer = new TextViewerWindow(
                    MessageText.getString( CFG_PREFIX	+ "testsocks.title" ),
                    null,
                    "Testing SOCKS connection to " + target.getHost() + ":" + target.getPort(), false  );
                final AESemaphore	test_done = new AESemaphore( "" );
                new AEThread2( "SOCKS test" ) {
                    public void
                    run() {
                        try {
                            String[] vers = target.getVersionsSupported();
                            String ver = "";
                            for ( String v: vers ) {
                                ver += (ver.length()==0?"":", ") + v;
                            }
                            appendText( viewer, "\r\nConnection OK - supported version(s): " + ver );
                        } catch( Throwable e ) {
                            appendText( viewer, "\r\n" + Debug.getNestedExceptionMessage( e ));
                        } finally {
                            test_done.release();
                        }
                    }
                } .start();
                new AEThread2( "SOCKS test dotter" ) {
                    public void
                    run() {
                        while( !test_done.reserveIfAvailable()) {
                            appendText( viewer, "." );
                            try {
                                Thread.sleep(500);
                            } catch( Throwable e ) {
                                break;
                            }
                        }
                    }
                } .start();
            }
        }
        private void
        appendText(
            final TextViewerWindow	viewer,
            final String			line ) {
            Utils.execSWTThread(
            new Runnable() {
                public void
                run() {
                    if ( !viewer.isDisposed()) {
                        viewer.append2( line );
                    }
                }
            });
        }
    });
    Parameter[] socks_params = { enableProxy, enableSocks, pHost, pPort, pUser, pPass };
    ParameterChangeAdapter socks_adapter =
    new ParameterChangeAdapter() {
        public void
        parameterChanged(
            Parameter	p,
            boolean		caused_internally ) {
            if ( test_socks.isDisposed()) {
                p.removeChangeListener( this );
            } else {
                if ( !caused_internally ) {
                    boolean 	enabled =
                        enableProxy.isSelected() &&
                        enableSocks.isSelected() &&
                        pHost.getValue().trim().length() > 0 &&
                        pPort.getValue().trim().length() > 0;
                    if ( enabled ) {
                        try {
                            int port = Integer.parseInt( pPort.getValue() );
                            NetworkAdminSocksProxy nasp =
                                NetworkAdmin.getSingleton().createSocksProxy(
                                    pHost.getValue(), port, pUser.getValue(),pPass.getValue());
                            synchronized( test_proxy ) {
                                test_proxy[0] = nasp;
                            }
                        } catch( Throwable e ) {
                            enabled = false;
                        }
                    }
                    if ( !enabled ) {
                        synchronized( test_proxy ) {
                            test_proxy[0] = null;
                        }
                    }
                    final boolean f_enabled = enabled;
                    Utils.execSWTThread(
                    new Runnable() {
                        public void
                        run() {
                            if ( !test_socks.isDisposed()) {
                                test_socks.setEnabled( f_enabled );
                            }
                        }
                    });
                }
            }
        }
    };
    for ( Parameter p: socks_params ) {
        p.addChangeListener( socks_adapter );
    }
    socks_adapter.parameterChanged( null, false );	// init settings
    ////////////////////////////////////////////////
    Group gProxyPeer = new Group(cSection, SWT.NULL);
    Messages.setLanguageText(gProxyPeer, CFG_PREFIX + "group.peer");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gProxyPeer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gProxyPeer.setLayout(layout);
    final BooleanParameter enableSocksPeer = new BooleanParameter(gProxyPeer,
            "Proxy.Data.Enable", CFG_PREFIX + "enable_socks.peer");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableSocksPeer.setLayoutData(gridData);
    final BooleanParameter socksPeerInform = new BooleanParameter(gProxyPeer,
            "Proxy.Data.SOCKS.inform", CFG_PREFIX + "peer.informtracker");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    socksPeerInform.setLayoutData(gridData);
    Label lSocksVersion = new Label(gProxyPeer, SWT.NULL);
    Messages.setLanguageText(lSocksVersion, CFG_PREFIX + "socks.version");
    String[] socks_types = { "V4", "V4a", "V5" };
    String dropLabels[] = new String[socks_types.length];
    String dropValues[] = new String[socks_types.length];
    for (int i = 0; i < socks_types.length; i++) {
        dropLabels[i] = socks_types[i];
        dropValues[i] = socks_types[i];
    }
    final StringListParameter socksType = new StringListParameter(gProxyPeer,
            "Proxy.Data.SOCKS.version", "V4", dropLabels, dropValues);
    final BooleanParameter sameConfig = new BooleanParameter(gProxyPeer,
            "Proxy.Data.Same", CFG_PREFIX + "peer.same");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    sameConfig.setLayoutData(gridData);
    Label lDataHost = new Label(gProxyPeer, SWT.NULL);
    Messages.setLanguageText(lDataHost, CFG_PREFIX + "host");
    StringParameter pDataHost = new StringParameter(gProxyPeer,
            "Proxy.Data.Host", "");
    gridData = new GridData();
    gridData.widthHint = 105;
    pDataHost.setLayoutData(gridData);
    Label lDataPort = new Label(gProxyPeer, SWT.NULL);
    Messages.setLanguageText(lDataPort, CFG_PREFIX + "port");
    StringParameter pDataPort = new StringParameter(gProxyPeer,
            "Proxy.Data.Port", "");
    gridData = new GridData();
    gridData.widthHint = 40;
    pDataPort.setLayoutData(gridData);
    Label lDataUser = new Label(gProxyPeer, SWT.NULL);
    Messages.setLanguageText(lDataUser, CFG_PREFIX + "username");
    StringParameter pDataUser = new StringParameter(gProxyPeer,
            "Proxy.Data.Username");
    gridData = new GridData();
    gridData.widthHint = 105;
    pDataUser.setLayoutData(gridData);
    Label lDataPass = new Label(gProxyPeer, SWT.NULL);
    Messages.setLanguageText(lDataPass, CFG_PREFIX + "password");
    StringParameter pDataPass = new StringParameter(gProxyPeer,
            "Proxy.Data.Password", "");
    gridData = new GridData();
    gridData.widthHint = 105;
    pDataPass.setLayoutData(gridData);
    final Control[] proxy_controls = new Control[] { enableSocks.getControl(),
            lHost, pHost.getControl(), lPort, pPort.getControl(), lUser,
            pUser.getControl(), lPass, pPass.getControl(),
                                                   };
    IAdditionalActionPerformer proxy_enabler = new GenericActionPerformer(
    new Control[] {}) {
        public void performAction() {
            for (int i = 0; i < proxy_controls.length; i++) {
                proxy_controls[i].setEnabled(enableProxy.isSelected());
            }
        }
    };
    final Control[] proxy_peer_controls = new Control[] { lDataHost,
            pDataHost.getControl(), lDataPort, pDataPort.getControl(), lDataUser,
            pDataUser.getControl(), lDataPass, pDataPass.getControl()
                                                        };
    final Control[] proxy_peer_details = new Control[] {
        sameConfig.getControl(), socksPeerInform.getControl(),
        socksType.getControl(), lSocksVersion
    };
    IAdditionalActionPerformer proxy_peer_enabler = new GenericActionPerformer(
    new Control[] {}) {
        public void performAction() {
            for (int i = 0; i < proxy_peer_controls.length; i++) {
                proxy_peer_controls[i].setEnabled(enableSocksPeer.isSelected()
                                                  && !sameConfig.isSelected());
            }
            for (int i = 0; i < proxy_peer_details.length; i++) {
                proxy_peer_details[i].setEnabled(enableSocksPeer.isSelected());
            }
        }
    };
    enableSocks.setAdditionalActionPerformer(proxy_enabler);
    enableProxy.setAdditionalActionPerformer(proxy_enabler);
    enableSocksPeer.setAdditionalActionPerformer(proxy_peer_enabler);
    sameConfig.setAdditionalActionPerformer(proxy_peer_enabler);
    // dns info
    Label label = new Label(cSection, SWT.WRAP);
    Messages.setLanguageText(label, CFG_PREFIX + "dns.info");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 200;  // needed for wrap
    label.setLayoutData(gridData);
    // check on start
    final BooleanParameter checkOnStart = new BooleanParameter(cSection,
            "Proxy.Check.On.Start", CFG_PREFIX + "check.on.start");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    checkOnStart.setLayoutData(gridData);
    // icon
    final BooleanParameter showIcon = new BooleanParameter(cSection,
            "Proxy.SOCKS.ShowIcon", CFG_PREFIX + "show_icon");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    showIcon.setLayoutData(gridData);
    final BooleanParameter flagIncoming = new BooleanParameter(cSection,
            "Proxy.SOCKS.ShowIcon.FlagIncoming", CFG_PREFIX + "show_icon.flag.incoming");
    gridData = new GridData();
    gridData.horizontalIndent=50;
    gridData.horizontalSpan = 2;
    flagIncoming.setLayoutData(gridData);
    showIcon.setAdditionalActionPerformer(
        new ChangeSelectionActionPerformer(flagIncoming));
    // username info
    label = new Label(cSection, SWT.WRAP);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    label.setText(MessageText.getString(CFG_PREFIX+"username.info" ));
    return cSection;
}