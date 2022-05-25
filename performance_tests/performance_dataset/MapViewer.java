private void displayMap(Graphics2D g) {
    final ClientOptions options = freeColClient.getClientOptions();
    final Player player = freeColClient.getMyPlayer();
    AffineTransform originTransform = g.getTransform();
    Rectangle clipBounds = g.getClipBounds();
    Map map = freeColClient.getGame().getMap();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
    /*
    PART 1
    ======
    Position the map if it is not positioned yet.
    */
    repositionMapIfNeeded();
    /*
    PART 1a
    =======
    Determine which tiles need to be redrawn.
    */
    int firstRow = (clipBounds.y - topRowY) / (halfHeight) - 1;
    int clipTopY = topRowY + firstRow * (halfHeight);
    firstRow = topRow + firstRow;
    int firstColumn = (clipBounds.x - leftColumnX) / tileWidth - 1;
    int clipLeftX = leftColumnX + firstColumn * tileWidth;
    firstColumn = leftColumn + firstColumn;
    int lastRow = (clipBounds.y + clipBounds.height - topRowY)
                  / (halfHeight);
    lastRow = topRow + lastRow;
    int lastColumn = (clipBounds.x + clipBounds.width - leftColumnX)
                     / tileWidth;
    lastColumn = leftColumn + lastColumn;
    /*
    PART 1b
    =======
    Create a GeneralPath to draw the grid with, if needed.
    */
    if (options.getBoolean(ClientOptions.DISPLAY_GRID)) {
        gridPath = new GeneralPath();
        gridPath.moveTo(0, 0);
        int nextX = halfWidth;
        int nextY = -halfHeight;
        for (int i = 0; i <= ((lastColumn - firstColumn) * 2 + 1); i++) {
            gridPath.lineTo(nextX, nextY);
            nextX += halfWidth;
            nextY = (nextY == 0 ? -halfHeight : 0);
        }
    }
    /*
    PART 2
    ======
    Display the Tiles and the Units.
    */
    g.setColor(Color.black);
    g.fillRect(clipBounds.x, clipBounds.y,
               clipBounds.width, clipBounds.height);
    /*
    PART 2a
    =======
    Display the base Tiles
    */
    g.translate(clipLeftX, clipTopY);
    AffineTransform baseTransform = g.getTransform();
    AffineTransform rowTransform = null;
    // Row per row; start with the top modified row
    for (int row = firstRow; row <= lastRow; row++) {
        rowTransform = g.getTransform();
        if (row % 2 == 1) {
            g.translate(halfWidth, 0);
        }
        // Column per column; start at the left side to display the tiles.
        for (int column = firstColumn; column <= lastColumn; column++) {
            Tile tile = map.getTile(column, row);
            displayBaseTile(g, lib, tile, true);
            g.translate(tileWidth, 0);
        }
        g.setTransform(rowTransform);
        g.translate(0, halfHeight);
    }
    g.setTransform(baseTransform);
    /*
    PART 2b
    =======
    Display the Tile overlays and Units
    */
    List<Unit> units = new ArrayList<Unit>();
    List<AffineTransform> unitTransforms = new ArrayList<AffineTransform>();
    List<Settlement> settlements = new ArrayList<Settlement>();
    List<AffineTransform> settlementTransforms
        = new ArrayList<AffineTransform>();
    int colonyLabels = options.getInteger(ClientOptions.COLONY_LABELS);
    boolean withNumbers = colonyLabels == ClientOptions.COLONY_LABELS_CLASSIC;
    // Row per row; start with the top modified row
    for (int row = firstRow; row <= lastRow; row++) {
        rowTransform = g.getTransform();
        if (row % 2 == 1) {
            g.translate(halfWidth, 0);
        }
        if (options.getBoolean(ClientOptions.DISPLAY_GRID)) {
            // Display the grid.
            g.translate(0, halfHeight);
            g.setStroke(gridStroke);
            g.setColor(Color.BLACK);
            g.draw(gridPath);
            g.translate(0, -halfHeight);
        }
        // Column per column; start at the left side to display the tiles.
        for (int column = firstColumn; column <= lastColumn; column++) {
            Tile tile = map.getTile(column, row);
            // paint full borders
            paintBorders(g, tile, BorderType.COUNTRY, true);
            // Display the Tile overlays:
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);
            displayTileOverlays(g, tile, true, withNumbers);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            // paint transparent borders
            paintBorders(g, tile, BorderType.COUNTRY, false);
            if (displayTileCursor(tile)) {
                drawCursor(g);
            }
            // check for units
            if (tile != null) {
                Unit unitInFront = getUnitInFront(tile);
                if (unitInFront != null && !isOutForAnimation(unitInFront)) {
                    units.add(unitInFront);
                    unitTransforms.add(g.getTransform());
                }
                // check for settlements
                Settlement settlement = tile.getSettlement();
                if (settlement != null) {
                    settlements.add(settlement);
                    settlementTransforms.add(g.getTransform());
                }
            }
            g.translate(tileWidth, 0);
        }
        g.setTransform(rowTransform);
        g.translate(0, halfHeight);
    }
    g.setTransform(baseTransform);
    /*
    PART 2c
    =======
    Display units
    */
    if (units.size() > 0) {
        g.setColor(Color.BLACK);
        final Image im = lib.getMiscImage(ImageLibrary.DARKNESS);
        for (int index = 0; index < units.size(); index++) {
            final Unit unit = units.get(index);
            g.setTransform(unitTransforms.get(index));
            if (unit.isUndead()) {
                // display darkness
                centerImage(g, im);
            }
            displayUnit(g, unit);
        }
        g.setTransform(baseTransform);
    }
    /*
    PART 3
    ======
    Display the colony names.
    */
    if (settlements.size() > 0
            && colonyLabels != ClientOptions.COLONY_LABELS_NONE) {
        for (int index = 0; index < settlements.size(); index++) {
            final Settlement settlement = settlements.get(index);
            if (settlement.isDisposed()) {
                logger.warning("Settlement display race detected: "
                               + settlement.getName());
                continue;
            }
            String name = Messages.message(settlement.getLocationNameFor(player));
            if (name == null) continue;
            Color backgroundColor = lib.getColor(settlement.getOwner());
            Font font = ResourceManager.getFont("NormalFont", 18f);
            Font productionFont = ResourceManager.getFont("NormalFont", 12f);
            // int yOffset = lib.getSettlementImage(settlement).getHeight(null) + 1;
            int yOffset = tileHeight;
            g.setTransform(settlementTransforms.get(index));
            switch (colonyLabels) {
            case ClientOptions.COLONY_LABELS_CLASSIC:
                Image img = lib.getStringImage(g, name, backgroundColor, font);
                g.drawImage(img, (tileWidth - img.getWidth(null))/2 + 1,
                            yOffset, null);
                break;
            case ClientOptions.COLONY_LABELS_MODERN:
            default:
                backgroundColor = new Color(backgroundColor.getRed(),
                                            backgroundColor.getGreen(),
                                            backgroundColor.getBlue(), 128);
                TextSpecification[] specs = new TextSpecification[1];
                if (settlement instanceof Colony
                        && settlement.getOwner() == player) {
                    Colony colony = (Colony) settlement;
                    BuildableType buildable = colony.getCurrentlyBuilding();
                    if (buildable != null) {
                        specs = new TextSpecification[2];
                        String t = Messages.message(buildable.getNameKey())
                                   + " " + Messages.getTurnsText(colony.getTurnsToComplete(buildable));
                        specs[1] = new TextSpecification(t, productionFont);
                    }
                }
                specs[0] = new TextSpecification(name, font);
                Image nameImage = createLabel(g, specs, backgroundColor);
                if (nameImage != null) {
                    int spacing = 3;
                    Image leftImage = null;
                    Image rightImage = null;
                    if (settlement instanceof Colony) {
                        String size = Integer.toString(((Colony)settlement)
                                                       .getDisplayUnitCount());
                        leftImage = createLabel(g, size, font,
                                                backgroundColor);
                        if (player.owns(settlement)) {
                            int bonusProduction = ((Colony)settlement).getProductionBonus();
                            if (bonusProduction != 0) {
                                String bonus = (bonusProduction > 0)
                                               ? "+" + bonusProduction
                                               : Integer.toString(bonusProduction);
                                rightImage = createLabel(g, bonus, font,
                                                         backgroundColor);
                            }
                        }
                    } else if (settlement instanceof IndianSettlement) {
                        IndianSettlement is = (IndianSettlement) settlement;
                        if (is.getType().isCapital()) {
                            leftImage = createCapitalLabel(nameImage.getHeight(null), 5, backgroundColor);
                        }
                        Unit missionary = is.getMissionary();
                        if (missionary != null) {
                            boolean expert = missionary.hasAbility(Ability.EXPERT_MISSIONARY);
                            backgroundColor = lib.getColor(missionary.getOwner());
                            backgroundColor = new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), 128);
                            rightImage = createReligiousMissionLabel(nameImage.getHeight(null), 5, backgroundColor, expert);
                        }
                    }
                    int width = (int)((nameImage.getWidth(null)
                                       * lib.getScalingFactor())
                                      + ((leftImage != null)
                                         ? (leftImage.getWidth(null)
                                            * lib.getScalingFactor()) + spacing
                                         : 0)
                                      + ((rightImage != null)
                                         ? (rightImage.getWidth(null)
                                            * lib.getScalingFactor()) + spacing
                                         : 0));
                    int labelOffset = (tileWidth - width)/2;
                    yOffset -= (nameImage.getHeight(null)
                                * lib.getScalingFactor())/2;
                    if (leftImage != null) {
                        g.drawImage(leftImage, labelOffset, yOffset, null);
                        labelOffset += (leftImage.getWidth(null)
                                        * lib.getScalingFactor()) + spacing;
                    }
                    g.drawImage(nameImage, labelOffset, yOffset, null);
                    if (rightImage != null) {
                        labelOffset += (nameImage.getWidth(null)
                                        * lib.getScalingFactor()) + spacing;
                        g.drawImage(rightImage, labelOffset, yOffset, null);
                    }
                    break;
                }
            }
        }
    }
    g.setTransform(originTransform);
    /*
    PART 4
    ======
    Display goto path
    */
    if (currentPath != null)
        displayGotoPath(g, currentPath);
    if (gotoPath != null)
        displayGotoPath(g, gotoPath);
    /*
    PART 5
    ======
    Grey out the map if it is not my turn (and a multiplayer game).
    */
    Canvas canvas = gui.getCanvas();
    if (!freeColClient.isMapEditor()
            && freeColClient.getGame() != null
            && !freeColClient.currentPlayerIsMyPlayer()) {
        if (greyLayer == null) greyLayer = new GrayLayer(lib);
        if (greyLayer.getParent() == null) { // Not added to the canvas yet.
            canvas.addToCanvas(greyLayer, JLayeredPane.DEFAULT_LAYER);
            canvas.moveToFront(greyLayer);
        }
        greyLayer.setBounds(0, 0, canvas.getSize().width,
                            canvas.getSize().height);
        greyLayer.setPlayer(freeColClient.getGame().getCurrentPlayer());
    } else {
        if (greyLayer != null && greyLayer.getParent() != null) {
            canvas.removeFromCanvas(greyLayer);
        }
    }
    /*
    PART 6
    ======
    Display the messages, if there are any.
    */
    if (getMessageCount() > 0) {
        // Don't edit the list of messages while I'm drawing them.
        synchronized (this) {
            Font font = ResourceManager.getFont("NormalFont", 12f);
            GUIMessage message = getMessage(0);
            Image si = lib.getStringImage(g, message.getMessage(),
                                          message.getColor(), font);
            int yy = size.height - 300 - getMessageCount()
                     * si.getHeight(null);
            int xx = 40;
            for (int i = 0; i < getMessageCount(); i++) {
                message = getMessage(i);
                g.drawImage(lib.getStringImage(g, message.getMessage(),
                                               message.getColor(), font),
                            xx, yy, null);
                yy += si.getHeight(null);
            }
        }
    }
    Image decoration = ResourceManager.getImage("menuborder.shadow.s.image");
    int width = decoration.getWidth(null);
    for (int index = 0; index < size.width; index += width) {
        g.drawImage(decoration, index, 0, null);
    }
    decoration = ResourceManager.getImage("menuborder.shadow.sw.image");
    g.drawImage(decoration, 0, 0, null);
    decoration = ResourceManager.getImage("menuborder.shadow.se.image");
    g.drawImage(decoration, size.width - decoration.getWidth(null), 0, null);
}