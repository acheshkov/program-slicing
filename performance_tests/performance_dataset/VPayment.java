private boolean saveChanges() {
    ValueNamePair vp = (ValueNamePair)paymentCombo.getSelectedItem();
    String newPaymentRule = vp.getValue();
    log.info("New Rule: " + newPaymentRule);
    //  only Payment Rule
    if (m_onlyRule) {
        if (!newPaymentRule.equals(m_PaymentRule))
            m_mTab.setValue("PaymentRule", newPaymentRule);
        return true;
    }
    //	New Values
    Timestamp newDateAcct = m_DateAcct;
    int newC_PaymentTerm_ID = m_C_PaymentTerm_ID;
    int newC_CashLine_ID = m_C_CashLine_ID;
    int newC_CashBook_ID = m_C_CashBook_ID;
    String newCCType = m_CCType;
    int newC_BankAccount_ID = 0;
    //	B (Cash)		(Currency)
    if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_Cash)) {
        KeyNamePair kp = (KeyNamePair)bCashBookCombo.getSelectedItem();
        if (kp != null)
            newC_CashBook_ID = kp.getKey();
        newDateAcct = (Timestamp)bDateField.getValue();
    }
    //	K (CreditCard)  Type, Number, Exp, Approval
    else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_CreditCard)) {
        vp = (ValueNamePair)kTypeCombo.getSelectedItem();
        if (vp != null)
            newCCType = vp.getValue();
    }
    //	T (Transfer)	BPartner_Bank
    else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_DirectDeposit)
             || newPaymentRule.equals(X_C_Order.PAYMENTRULE_DirectDebit) ) {
        //	KeyNamePair kp = (KeyNamePair)tAccountCombo.getSelectedItem();
        String tAccountNo = tAccountNoField.getText();
        String tRoutingNo = tRoutingNoField.getText();
        String tIBAN = tIBANField.getText();
        if (m_bpba == null) {
            m_bpba = new MBPBankAccount(Env.getCtx(), m_C_BPartner_ID,
                                        tAccountNo, tRoutingNo, tIBAN, null);
            m_bpba.setA_Name(tBankNameField.getText());
            m_bpba.setA_City(tBankCityField.getText());
            m_bpba.save();
        } else {
            m_bpba.setA_Name(tBankNameField.getText());
            m_bpba.setA_City(tBankCityField.getText());
            if (m_bpba.updateInfo(tAccountNo, tRoutingNo, tIBAN))
                m_bpba.save();
        }
        //	only save if Transfer
        m_mTab.setValue("C_BP_BankAccount_ID", Integer.valueOf(m_bpba.getC_BP_BankAccount_ID()));
    }
    //	P (PaymentTerm)	PaymentTerm
    else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_OnCredit)) {
        KeyNamePair kp = (KeyNamePair)pTermCombo.getSelectedItem();
        if (kp != null)
            newC_PaymentTerm_ID = kp.getKey();
    }
    //	S (Check)		(Currency) CheckNo, Routing
    else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_Check)) {
        //	sCurrencyCombo.getSelectedItem();
        KeyNamePair kp = (KeyNamePair)sBankAccountCombo.getSelectedItem();
        if (kp != null)
            newC_BankAccount_ID = kp.getKey();
    } else
        return false;
    //  find Bank Account if not qualified yet
    if ("KTSD".indexOf(newPaymentRule) != -1 && newC_BankAccount_ID == 0) {
        if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_DirectDeposit)) {
        } else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_DirectDebit)) {
        } else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_Check)) {
        }
    }
    /***********************
    *  Changed PaymentRule
    */
    if (!newPaymentRule.equals(m_PaymentRule)) {
        log.fine("Changed PaymentRule: " + m_PaymentRule + " -> " + newPaymentRule);
        //  We had a CashBook Entry
        if (m_PaymentRule.equals(X_C_Order.PAYMENTRULE_Cash)) {
            log.fine("Old Cash - " + m_cashLine);
            if (m_cashLine != null) {
                MCashLine cl = m_cashLine.createReversal();
                if (cl.save())
                    log.config( "CashCancelled");
                else
                    ADialog.error(m_WindowNo, this, "PaymentError", "CashNotCancelled");
            }
            newC_CashLine_ID = 0;      //  reset
        }
        //  We had a change in Payment type (e.g. Check to CC)
        else if ("KTSD".indexOf(m_PaymentRule) != -1 && "KTSD".indexOf(newPaymentRule) != -1 && m_mPaymentOriginal != null) {
            log.fine("Old Payment(1) - " + m_mPaymentOriginal);
            m_mPaymentOriginal.setDocAction(DocActionConstants.ACTION_Reverse_Correct);
            boolean ok = m_mPaymentOriginal.processIt(DocActionConstants.ACTION_Reverse_Correct);
            m_mPaymentOriginal.save();
            if (ok)
                log.info( "Payment Canecelled - " + m_mPaymentOriginal);
            else
                ADialog.error(m_WindowNo, this, "PaymentError", "PaymentNotCancelled " + m_mPaymentOriginal.getDocumentNo());
            m_mPayment.resetNew();
        }
        //	We had a Payment and something else (e.g. Check to Cash)
        else if ("KTSD".indexOf(m_PaymentRule) != -1 && "KTSD".indexOf(newPaymentRule) == -1) {
            log.fine("Old Payment(2) - " + m_mPaymentOriginal);
            if (m_mPaymentOriginal != null) {
                m_mPaymentOriginal.setDocAction(DocActionConstants.ACTION_Reverse_Correct);
                boolean ok = m_mPaymentOriginal.processIt(DocActionConstants.ACTION_Reverse_Correct);
                m_mPaymentOriginal.save();
                if (ok) {      //  Cancel Payment
                    log.fine("PaymentCancelled " + m_mPayment.getDocumentNo ());
                    m_mTab.getTableModel().dataSave(true);
                    m_mPayment.resetNew();
                    m_mPayment.setAmount(m_C_Currency_ID, m_Amount);
                } else
                    ADialog.error(m_WindowNo, this, "PaymentError", "PaymentNotCancelled " + m_mPayment.getDocumentNo());
            }
        }
    }
    //  Get Order and optionally Invoice
    int C_Order_ID = Env.getCtx().getContextAsInt( m_WindowNo, "C_Order_ID");
    int C_Invoice_ID = Env.getCtx().getContextAsInt( m_WindowNo, "C_Invoice_ID");
    if (C_Invoice_ID == 0 && m_DocStatus.equals("CO"))
        C_Invoice_ID = getInvoiceID (C_Order_ID);
    //  Amount sign negative, if ARC (Credit Memo) or API (AP Invoice)
    boolean negateAmt = false;
    MInvoice invoice = null;
    if (C_Invoice_ID != 0) {
        invoice = new MInvoice (Env.getCtx(), C_Invoice_ID, null);
        negateAmt = invoice.isCreditMemo();
    }
    MOrder order = null;
    if (invoice == null && C_Order_ID != 0)
        order = new MOrder (Env.getCtx(), C_Order_ID, null);
    BigDecimal payAmount = m_Amount;
    if (negateAmt)
        payAmount = m_Amount.negate();
    // Info
    log.config("C_Order_ID=" + C_Order_ID + ", C_Invoice_ID=" + C_Invoice_ID + ", NegateAmt=" + negateAmt);
    /***********************
    *  CashBook
    */
    if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_Cash)) {
        log.fine("Cash");
        if (C_Invoice_ID == 0 && order == null) {
            log.config("No Invoice!");
            ADialog.error(m_WindowNo, this, "PaymentError", "CashNotCreated");
        } else {
            //  Changed Amount
            if (m_cashLine != null
                    && payAmount.compareTo(m_cashLine.getAmount()) != 0) {
                log.config("Changed CashBook Amount");
                m_cashLine.setAmount(payAmount);
                if (m_cashLine.save())
                    log.config("CashAmt Changed");
            }
            //	Different Date/CashBook
            if (m_cashLine != null
                    && (newC_CashBook_ID != m_C_CashBook_ID
                        || !TimeUtil.isSameDay(m_cashLine.getStatementDate(), newDateAcct))) {
                log.config("Changed CashBook/Date: " + m_C_CashBook_ID + "->" + newC_CashBook_ID);
                MCashLine reverse = m_cashLine.createReversal();
                if (!reverse.save())
                    ADialog.error(m_WindowNo, this, "PaymentError", "CashNotCancelled");
                m_cashLine = null;
            }
            //	Create new
            if (m_cashLine == null) {
                log.config("New CashBook");
                int C_Currency_ID = 0;
                if (invoice != null)
                    C_Currency_ID = invoice.getC_Currency_ID();
                if (C_Currency_ID == 0 && order != null)
                    C_Currency_ID = order.getC_Currency_ID();
                MCash cash = null;
                if (newC_CashBook_ID != 0)
                    cash = MCash.get (Env.getCtx(), newC_CashBook_ID, newDateAcct, null);
                else	//	Default
                    cash = MCash.get (Env.getCtx(), m_AD_Org_ID, newDateAcct, C_Currency_ID, null);
                if (cash == null || cash.get_ID() == 0)
                    ADialog.error(m_WindowNo, this, "PaymentError", "CashNotCreated");
                else {
                    MCashLine cl = new MCashLine (cash);
                    if (invoice != null)
                        cl.setInvoice(invoice);
                    if (order != null) {
                        cl.setOrder(order, null);
                        m_needSave = true;
                    }
                    if (cl.save()) {
                        newC_CashLine_ID = cl.getC_CashLine_ID();
                        log.config("CashCreated");
                    } else
                        ADialog.error(m_WindowNo, this, "PaymentError", "CashNotCreated");
                }
            }
        }	//	have invoice
    }
    /***********************
    *  Payments
    */
    if ("KS".indexOf(newPaymentRule) != -1) {
        log.fine("Payment - " + newPaymentRule);
        //  Set Amount
        m_mPayment.setAmount(m_C_Currency_ID, payAmount);
        if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_CreditCard)) {
            m_mPayment.setCreditCard(X_C_Payment.TRXTYPE_Sales, newCCType,
                                     kNumberField.getText(), "", kExpField.getText());
            m_mPayment.setPaymentProcessor();
        } else if (newPaymentRule.equals(X_C_Order.PAYMENTRULE_Check)) {
            m_mPayment.setBankCheck(newC_BankAccount_ID, m_isSOTrx, sRoutingField.getText(),
                                    sNumberField.getText(), sCheckField.getText());
        }
        m_mPayment.setC_BPartner_ID(m_C_BPartner_ID);
        m_mPayment.setC_Invoice_ID(C_Invoice_ID);
        if (order != null) {
            m_mPayment.setC_Order_ID(C_Order_ID);
            m_needSave = true;
        }
        m_mPayment.setDateTrx(m_DateAcct);
        m_mPayment.setDateAcct(m_DateAcct);
        m_mPayment.save();
        //  Save/Post
        if (X_C_Payment.DOCSTATUS_Drafted.equals(m_mPayment.getDocStatus())) {
            boolean ok = m_mPayment.processIt(DocActionConstants.ACTION_Complete);
            m_mPayment.save();
            if (ok)
                ADialog.info(m_WindowNo, this, "PaymentCreated", m_mPayment.getDocumentNo());
            else
                ADialog.error(m_WindowNo, this, "PaymentError", "PaymentNotCreated");
        } else
            log.fine("NotDraft " + m_mPayment);
    }
    /**********************
    *	Save Values to mTab
    */
    log.config("Saving changes");
    //
    if (!newPaymentRule.equals(m_PaymentRule))
        m_mTab.setValue("PaymentRule", newPaymentRule);
    //
    if (!newDateAcct.equals(m_DateAcct))
        m_mTab.setValue("DateAcct", newDateAcct);
    //
    if (newC_PaymentTerm_ID != m_C_PaymentTerm_ID)
        m_mTab.setValue("C_PaymentTerm_ID", Integer.valueOf(newC_PaymentTerm_ID));
    //	Set Payment
    if (m_mPayment.getC_Payment_ID() != m_C_Payment_ID) {
        if (m_mPayment.getC_Payment_ID() == 0)
            m_mTab.setValue("C_Payment_ID", null);
        else
            m_mTab.setValue("C_Payment_ID", Integer.valueOf(m_mPayment.getC_Payment_ID()));
    }
    //	Set Cash
    if (newC_CashLine_ID != m_C_CashLine_ID) {
        if (newC_CashLine_ID == 0)
            m_mTab.setValue("C_CashLine_ID", null);
        else
            m_mTab.setValue("C_CashLine_ID", Integer.valueOf(newC_CashLine_ID));
    }
    return true;
}