private void updateColony(Colony colony) {
    final Specification spec = getSpecification();
    final GoodsType foodType = spec.getPrimaryFoodType();
    final UnitType colonistType = spec.getDefaultUnitType();
    final ImageLibrary lib = getGUI().getImageLibrary();
    // Assemble the fundamental facts about this colony
    final String cac = colony.getId();
    List<Tile> exploreTiles = new ArrayList<Tile>();
    List<Tile> clearTiles = new ArrayList<Tile>();
    List<Tile> plowTiles = new ArrayList<Tile>();
    List<Tile> roadTiles = new ArrayList<Tile>();
    colony.getColonyTileTodo(exploreTiles, clearTiles, plowTiles,
                             roadTiles);
    boolean plowMe = plowTiles.size() > 0
                     && plowTiles.get(0) == colony.getTile();
    int newColonist;
    boolean famine;
    if (colony.getGoodsCount(foodType) > Settlement.FOOD_PER_COLONIST) {
        famine = false;
        newColonist = 1;
    } else {
        int newFood = colony.getAdjustedNetProductionOf(foodType);
        famine = newFood < 0
                 && (colony.getGoodsCount(foodType) / -newFood) <= 3;
        newColonist = (newFood == 0) ? 0
                      : (newFood < 0) ? colony.getGoodsCount(foodType) / newFood - 1
                      : (Settlement.FOOD_PER_COLONIST
                         - colony.getGoodsCount(foodType)) / newFood + 1;
    }
    int grow = colony.getPreferredSizeChange();
    int bonus = colony.getProductionBonus();
    // Field: A button for the colony.
    // Colour: bonus in {-2,2} => {alarm, warn, plain, export, good}
    // Font: Bold if famine is threatening.
    JButton b = colourButton(cac, colony.getName(), null,
                             (bonus <= -2) ? cAlarm
                             : (bonus == -1) ? cWarn
                             : (bonus == 0) ? cPlain
                             : (bonus == 1) ? cExport
                             : cGood,
                             null);
    if (famine) {
        b.setFont(b.getFont().deriveFont(Font.BOLD));
    }
    reportPanel.add(b, "newline");
    // Field: The number of colonists that can be added to a
    // colony without damaging the production bonus, unless
    // the colony is inefficient in which case add the number
    // of colonists to remove to fix the inefficiency.
    // Colour: Blue if efficient/Red if inefficient.
    if (grow < 0) {
        b = colourButton(cac, Integer.toString(-grow), null, cAlarm,
                         stpl("report.colony.shrinking.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%amount%", -grow));
        reportPanel.add(b);
    } else if (grow > 0) {
        b = colourButton(cac, Integer.toString(grow), null, cGood,
                         stpl("report.colony.growing.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%amount%", grow));
        reportPanel.add(b);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Field: The number of potential colony tiles that need
    // exploring.
    // Colour: Always cAlarm
    if (exploreTiles.size() > 0) {
        b = colourButton(cac, Integer.toString(exploreTiles.size()),
                         null, cAlarm,
                         stpl("report.colony.exploring.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%amount%", exploreTiles.size()));
        reportPanel.add(b);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Field: The number of existing colony tiles that would
    // benefit from ploughing.
    // Colour: Always cAlarm
    // Font: Bold if one of the tiles is the colony center.
    if (plowTiles.size() > 0) {
        b = colourButton(cac, Integer.toString(plowTiles.size()),
                         null, cAlarm,
                         stpl("report.colony.plowing.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%amount%", plowTiles.size()));
        if (plowMe) {
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
        reportPanel.add(b);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Field: The number of existing colony tiles that would
    // benefit from a road.
    // Colour: cAlarm
    if (roadTiles.size() > 0) {
        b = colourButton(cac, Integer.toString(roadTiles.size()),
                         null, cAlarm,
                         stpl("report.colony.roadBuilding.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%amount%", roadTiles.size()));
        reportPanel.add(b);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Fields: The net production of each storable+non-trade-goods
    // goods type.
    // Colour: cAlarm if too low, cWarn if negative, empty if no
    // production, cPlain if production balanced at zero,
    // otherwise must be positive, wherein cExport
    // if exported, cAlarm if too high, else cGood.
    final int adjustment = colony.getWarehouseCapacity()
                           / GoodsContainer.CARGO_SIZE;
    for (GoodsType g : goodsTypes) {
        int p = colony.getAdjustedNetProductionOf(g);
        ExportData exportData = colony.getExportData(g);
        int low = exportData.getLowLevel() * adjustment;
        int high = exportData.getHighLevel() * adjustment;
        int amount = colony.getGoodsCount(g);
        Color c;
        StringTemplate tip;
        if (p < 0) {
            if (amount < low) {
                int turns = -amount / p + 1;
                c = cAlarm;
                tip = stpl("report.colony.production.low.description")
                      .addName("%colony%", colony.getName())
                      .add("%goods%", g.getNameKey())
                      .addAmount("%amount%", p)
                      .addAmount("%turns%", turns);
            } else {
                c = cWarn;
                tip = stpl("report.colony.production.description")
                      .addName("%colony%", colony.getName())
                      .add("%goods%", g.getNameKey())
                      .addAmount("%amount%", p);
            }
        } else if (p == 0) {
            if (colony.getTotalProductionOf(g) == 0) {
                c = null;
                tip = null;
            } else {
                c = cPlain;
                tip = stpl("report.colony.production.description")
                      .addName("%colony%", colony.getName())
                      .add("%goods%", g.getNameKey())
                      .addAmount("%amount%", p);
            }
        } else if (exportData.isExported()) {
            c = cExport;
            tip = stpl("report.colony.production.export.description")
                  .addName("%colony%", colony.getName())
                  .add("%goods%", g.getNameKey())
                  .addAmount("%amount%", p)
                  .addAmount("%export%", exportData.getExportLevel());
        } else if (g != foodType
                   && amount + p > colony.getWarehouseCapacity()) {
            c = cAlarm;
            int waste = amount + p - colony.getWarehouseCapacity();
            tip = stpl("report.colony.production.waste.description")
                  .addName("%colony%", colony.getName())
                  .add("%goods%", g.getNameKey())
                  .addAmount("%amount%", p)
                  .addAmount("%waste%", waste);
        } else if (g != foodType && amount > high) {
            int turns = (colony.getWarehouseCapacity() - amount) / p;
            c = cWarn;
            tip = stpl("report.colony.production.high.description")
                  .addName("%colony%", colony.getName())
                  .add("%goods%", g.getNameKey())
                  .addAmount("%amount%", p)
                  .addAmount("%turns%", turns);
        } else {
            c = cGood;
            tip = stpl("report.colony.production.description")
                  .addName("%colony%", colony.getName())
                  .add("%goods%", g.getNameKey())
                  .addAmount("%amount%", p);
        }
        if (c == null) reportPanel.add(new JLabel(""));
        else {
            b = colourButton(cac, Integer.toString(p), null, c, tip);
            reportPanel.add(b);
        }
    }
    // Collect the types of the units at work in the colony
    // (colony tiles and buildings) that are suboptimal (and
    // are not just temporarily there because they are being
    // taught), the types for sites that really need a new
    // unit, the teachers, and the units that are not working.
    // TODO: this needs to be merged with the requirements
    // checking code, but that in turn should be opened up
    // so the AI can use it...
    HashMap<UnitType, Suggestion> improve
        = new HashMap<UnitType, Suggestion>();
    HashMap<UnitType, Suggestion> want
        = new HashMap<UnitType, Suggestion>();
    List<Unit> teachers = new ArrayList<Unit>();
    List<Unit> notWorking = new ArrayList<Unit>();
    for (Unit u : colony.getTile().getUnitList()) {
        if (u.getState() != Unit.UnitState.FORTIFIED
                && u.getState() != Unit.UnitState.SENTRY) {
            notWorking.add(u);
        }
    }
    for (WorkLocation wl : colony.getAvailableWorkLocations()) {
        if (!wl.canBeWorked()) {
            continue;
        } else if (wl.canTeach()) {
            teachers.addAll(wl.getUnitList());
            continue;
        }
        UnitType expert;
        GoodsType work;
        boolean needsWorker = !wl.isFull();
        int delta;
        // Check first if the units are working, and then add a
        // suggestion if there is a better type of unit for the
        // work being done.
        for (Unit u : wl.getUnitList()) {
            if (u.getTeacher() != null) {
                continue; // Ignore students, they are temporary
            } else if ((work = u.getWorkType()) == null) {
                notWorking.add(u);
                needsWorker = true;
            } else if ((expert = spec.getExpertForProducing(work)) != null
                       && expert != u.getType()
                       && (delta = wl.getPotentialProduction(work, expert)
                                   - wl.getPotentialProduction(work, u.getType())) > 0
                       && wantGoods(wl, work, u, expert)) {
                addSuggestion(improve, u.getType(), expert,
                              work, delta);
            }
        }
        // Add a suggestion for an extra worker if there is
        // space, valid work to do, an expert type to do it,
        // and the goods are wanted.
        if (needsWorker
                && (work = bestProduction(wl, colonistType)) != null
                && (expert = spec.getExpertForProducing(work)) != null
                && (delta = wl.getPotentialProduction(work, expert)) > 0
                && wantGoods(wl, work, null, expert)) {
            addSuggestion(want, null, expert, work, delta);
        }
    }
    // Make a list of unit types that are not working at their
    // speciality, including the units just standing around.
    List<UnitType> couldWork = new ArrayList<UnitType>();
    for (Unit u : notWorking) {
        GoodsType t = u.getWorkType();
        WorkLocation wl = (u.getLocation() instanceof WorkLocation)
                          ? (WorkLocation) u.getLocation()
                          : null;
        GoodsType w = bestProduction(wl, colonistType);
        if (w == null || w != t) couldWork.add(u.getType());
    }
    // Field: New colonist arrival or famine warning.
    // Colour: cGood if arriving eventually, blank if not enough food
    // to grow, cWarn if negative, cAlarm if famine soon.
    if (newColonist > 0) {
        b = colourButton(cac, Integer.toString(newColonist),
                         null, cGood,
                         stpl("report.colony.arriving.description")
                         .addName("%colony%", colony.getName())
                         .add("%unit%", colonistType.getNameKey())
                         .addAmount("%turns%", newColonist));
        reportPanel.add(b);
    } else if (newColonist < 0) {
        b = colourButton(cac, Integer.toString(-newColonist),
                         null, (newColonist >= -3) ? cAlarm : cWarn,
                         stpl("report.colony.starving.description")
                         .addName("%colony%", colony.getName())
                         .addAmount("%turns%", -newColonist));
        reportPanel.add(b);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Field: What is currently being built (clickable if on the
    // buildqueue) and the turns until it completes, including
    // units being taught.
    // Colour: cAlarm bold "Nothing" if nothing being built, cAlarm
    // with no turns if no production, cGood with turns if
    // completing, cAlarm with turns if will block, turns
    // indicates when blocking occurs.
    BuildableType build = colony.getCurrentlyBuilding();
    int fields = 1 + teachers.size();
    String layout = (fields > 1) ? "split " + fields : null;
    String qac = BUILDQUEUE + colony.getId();
    if (build == null) {
        b = colourButton(qac, Messages.message("nothing"),
                         null, cAlarm,
                         stpl("report.colony.making.noconstruction.description")
                         .addName("%colony%", colony.getName()));
        b.setFont(b.getFont().deriveFont(Font.BOLD));
    } else {
        AbstractGoods needed = new AbstractGoods();
        int turns = colony.getTurnsToComplete(build, needed);
        String name = Messages.message(build.getNameKey());
        if (turns == FreeColObject.UNDEFINED) {
            b = colourButton(qac, name, null, cAlarm,
                             stpl("report.colony.making.noconstruction.description")
                             .addName("%colony%", colony.getName()));
        } else if (turns >= 0) {
            name += " " + Integer.toString(turns);
            b = colourButton(qac, name, null, cGood,
                             stpl("report.colony.making.constructing.description")
                             .addName("%colony%", colony.getName())
                             .add("%buildable%", build.getNameKey())
                             .addAmount("%turns%", turns));;
        } else if (turns < 0) {
            GoodsType goodsType = needed.getType();
            int goodsAmount = needed.getAmount()
                              - colony.getGoodsCount(goodsType);
            turns = -turns;
            name += " " + Integer.toString(turns);
            b = colourButton(qac, name, null, cAlarm,
                             stpl("report.colony.making.blocking.description")
                             .addName("%colony%", colony.getName())
                             .addAmount("%amount%", goodsAmount)
                             .add("%goods%", goodsType.getNameKey())
                             .add("%buildable%", build.getNameKey())
                             .addAmount("%turns%", turns));
        }
    }
    reportPanel.add(b, layout);
    layout = null;
    Collections.sort(teachers, teacherComparator);
    for (Unit u : teachers) {
        int left = u.getNeededTurnsOfTraining()
                   - u.getTurnsOfTraining();
        if (left <= 0) {
            b = colourButton(cac, Integer.toString(0),
                             lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                                                  true, 0.333), cAlarm,
                             stpl("report.colony.making.noteach.description")
                             .addName("%colony%", colony.getName())
                             .addStringTemplate("%teacher%", u.getLabel()));
        } else {
            b = colourButton(cac, Integer.toString(left),
                             lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                                                  true, 0.333), Color.BLACK,
                             stpl("report.colony.making.educating.description")
                             .addName("%colony%", colony.getName())
                             .addStringTemplate("%teacher%", u.getLabel())
                             .addAmount("%turns%", left));
        }
        reportPanel.add(b);
    }
    if (fields <= 0) reportPanel.add(new JLabel(""));
    // Field: The units that could be upgraded.
    if (!improve.isEmpty()) {
        addUnits(improve, couldWork, colony, grow);
    } else {
        reportPanel.add(new JLabel(""));
    }
    // Field: The units the colony could make good use of.
    if (!want.isEmpty()) {
        // TODO: explain food limitations better
        grow = Math.min(grow, colony.getNetProductionOf(foodType)
                        / Settlement.FOOD_PER_COLONIST);
        addUnits(want, couldWork, colony, grow);
    } else {
        reportPanel.add(new JLabel(""));
    }
}