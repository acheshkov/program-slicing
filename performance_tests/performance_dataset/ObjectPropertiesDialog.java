private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;
    jTabbedPane1 = new javax.swing.JTabbedPane();
    jPanel1 = new javax.swing.JPanel();
    jLabelUriString = new javax.swing.JLabel();
    jTextFieldUriString = new javax.swing.JTextField();
    jLabelName = new javax.swing.JLabel();
    jTextFieldName = new javax.swing.JTextField();
    jSeparator1 = new javax.swing.JSeparator();
    jLabelLabel = new javax.swing.JLabel();
    jTextFieldLabel = new javax.swing.JTextField();
    jLabelDescription = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    jEditorPaneDescription = new javax.swing.JEditorPane();
    jPanelResourceFile = new javax.swing.JPanel();
    jLabelPreview = new javax.swing.JLabel();
    jSeparator3 = new javax.swing.JSeparator();
    jCheckBoxChangeFile = new javax.swing.JCheckBox();
    jTextFieldFile = new javax.swing.JTextField();
    jButton1 = new javax.swing.JButton();
    jButtonCurrentReport = new javax.swing.JButton();
    jSeparator4 = new javax.swing.JSeparator();
    jButton2 = new javax.swing.JButton();
    jPanelSpacer = new javax.swing.JPanel();
    jPanelDescriptor = new javax.swing.JPanel();
    jScrollPane2 = new javax.swing.JScrollPane();
    jTextPaneDescriptor = new javax.swing.JTextPane();
    jPanel2 = new javax.swing.JPanel();
    jButtonSave = new javax.swing.JButton();
    jButtonClose = new javax.swing.JButton();
    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent evt) {
            formWindowClosing(evt);
        }
    });
    getContentPane().setLayout(new java.awt.GridBagLayout());
    jPanel1.setPreferredSize(new java.awt.Dimension(350, 250));
    jPanel1.setLayout(new java.awt.GridBagLayout());
    jLabelUriString.setText("Location (URI)"); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    jPanel1.add(jLabelUriString, gridBagConstraints);
    jTextFieldUriString.setEditable(false);
    jTextFieldUriString.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    jTextFieldUriString.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    jPanel1.add(jTextFieldUriString, gridBagConstraints);
    jLabelName.setText("ID"); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
    jPanel1.add(jLabelName, gridBagConstraints);
    jTextFieldName.setEditable(false);
    jTextFieldName.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    jTextFieldName.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
    jPanel1.add(jTextFieldName, gridBagConstraints);
    jSeparator1.setMinimumSize(new java.awt.Dimension(0, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 6, 4);
    jPanel1.add(jSeparator1, gridBagConstraints);
    jLabelLabel.setText("Name"); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
    jPanel1.add(jLabelLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    jPanel1.add(jTextFieldLabel, gridBagConstraints);
    jLabelDescription.setText("Description"); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
    jPanel1.add(jLabelDescription, gridBagConstraints);
    jScrollPane1.setViewportView(jEditorPaneDescription);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
    jPanel1.add(jScrollPane1, gridBagConstraints);
    jTabbedPane1.addTab("General", jPanel1);
    jPanelResourceFile.setLayout(new java.awt.GridBagLayout());
    jLabelPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabelPreview.setText("  "); // NOI18N
    jLabelPreview.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
    jLabelPreview.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    jLabelPreview.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
    jPanelResourceFile.add(jLabelPreview, gridBagConstraints);
    jSeparator3.setMinimumSize(new java.awt.Dimension(0, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 6, 4);
    jPanelResourceFile.add(jSeparator3, gridBagConstraints);
    jCheckBoxChangeFile.setText("Replace resource with this file:"); // NOI18N
    jCheckBoxChangeFile.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    jCheckBoxChangeFile.setMargin(new java.awt.Insets(0, 0, 0, 0));
    jCheckBoxChangeFile.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jCheckBoxChangeFileActionPerformed(evt);
        }
    });
    jCheckBoxChangeFile.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            jCheckBoxChangeFileStateChanged(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
    jPanelResourceFile.add(jCheckBoxChangeFile, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 24, 0, 0);
    jPanelResourceFile.add(jTextFieldFile, gridBagConstraints);
    jButton1.setText("Browse"); // NOI18N
    jButton1.setMinimumSize(new java.awt.Dimension(73, 19));
    jButton1.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridy = 3;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
    jPanelResourceFile.add(jButton1, gridBagConstraints);
    jButtonCurrentReport.setText("Current Report"); // NOI18N
    jButtonCurrentReport.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonCurrentReportActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 24, 0, 0);
    jPanelResourceFile.add(jButtonCurrentReport, gridBagConstraints);
    jSeparator4.setMinimumSize(new java.awt.Dimension(0, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 6, 4);
    jPanelResourceFile.add(jSeparator4, gridBagConstraints);
    jButton2.setText("Export file"); // NOI18N
    jButton2.setMinimumSize(new java.awt.Dimension(73, 23));
    jButton2.setPreferredSize(new java.awt.Dimension(100, 23));
    jButton2.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed1(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
    jPanelResourceFile.add(jButton2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.weighty = 1.0;
    jPanelResourceFile.add(jPanelSpacer, gridBagConstraints);
    jTabbedPane1.addTab("Resource", jPanelResourceFile);
    jPanelDescriptor.setLayout(new java.awt.GridBagLayout());
    jTextPaneDescriptor.setEditable(false);
    jScrollPane2.setViewportView(jTextPaneDescriptor);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    jPanelDescriptor.add(jScrollPane2, gridBagConstraints);
    jTabbedPane1.addTab("Descriptor", jPanelDescriptor);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
    getContentPane().add(jTabbedPane1, gridBagConstraints);
    jPanel2.setMinimumSize(new java.awt.Dimension(10, 30));
    jPanel2.setPreferredSize(new java.awt.Dimension(10, 30));
    jPanel2.setLayout(new java.awt.GridBagLayout());
    jButtonSave.setText("Save"); // NOI18N
    jButtonSave.setEnabled(false);
    jButtonSave.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonSaveActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
    jPanel2.add(jButtonSave, gridBagConstraints);
    jButtonClose.setText("Close"); // NOI18N
    jButtonClose.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonCloseActionPerformed(evt);
        }
    });
    jPanel2.add(jButtonClose, new java.awt.GridBagConstraints());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
    getContentPane().add(jPanel2, gridBagConstraints);
    pack();
}