private void createIndianSettlements(final Map map, List<Player> players) {
    Specification spec = map.getGame().getSpecification();
    float shares = 0f;
    List<IndianSettlement> settlements = new ArrayList<IndianSettlement>();
    List<Player> indians = new ArrayList<Player>();
    HashMap<String, Territory> territoryMap
        = new HashMap<String, Territory>();
    for (Player player : players) {
        if (!player.isIndian()) continue;
        switch (((IndianNationType) player.getNationType())
                .getNumberOfSettlements()) {
        case HIGH:
            shares += 4;
            break;
        case AVERAGE:
            shares += 3;
            break;
        case LOW:
            shares += 2;
            break;
        }
        indians.add(player);
        List<String> regionNames
            = ((IndianNationType) player.getNationType()).getRegionNames();
        Territory territory = null;
        if (regionNames == null || regionNames.isEmpty()) {
            territory = new Territory(player, terrainGenerator.getRandomLandTile(map, random).getPosition());
            territoryMap.put(player.getId(), territory);
        } else {
            for (String name : regionNames) {
                if (territoryMap.get(name) == null) {
                    ServerRegion region = (ServerRegion) map.getRegion(name);
                    if (region == null) {
                        territory = new Territory(player, terrainGenerator.getRandomLandTile(map, random).getPosition());
                    } else {
                        territory = new Territory(player, region);
                    }
                    territoryMap.put(name, territory);
                    logger.fine("Allocated region " + name
                                + " for " + player
                                + ". Center is " + territory.getCenter()
                                + ".");
                    break;
                }
            }
            if (territory == null) {
                logger.warning("Failed to allocate preferred region " + regionNames.get(0)
                               + " for " + player.getNation());
                outer: for (String name : regionNames) {
                    Territory otherTerritory = territoryMap.get(name);
                    for (String otherName : ((IndianNationType) otherTerritory.player.getNationType())
                            .getRegionNames()) {
                        if (territoryMap.get(otherName) == null) {
                            ServerRegion foundRegion = otherTerritory.region;
                            otherTerritory.region = (ServerRegion) map.getRegion(otherName);
                            territoryMap.put(otherName, otherTerritory);
                            territory = new Territory(player, foundRegion);
                            territoryMap.put(name, territory);
                            break outer;
                        }
                    }
                }
                if (territory == null) {
                    logger.warning("Unable to find free region for "
                                   + player.getName());
                    territory = new Territory(player, terrainGenerator.getRandomLandTile(map, random).getPosition());
                    territoryMap.put(player.getId(), territory);
                }
            }
        }
    }
    if (indians.isEmpty()) return;
    // Examine all the non-polar settleable tiles in a random
    // order picking out as many as possible suitable tiles for
    // native settlements such that can be guaranteed at least one
    // layer of surrounding tiles to own.
    int minSettlementDistance
        = spec.getRangeOption("model.option.settlementNumber").getValue();
    List<Tile> settlementTiles = new ArrayList<Tile>();
    tiles: for (Tile tile : map.getAllTiles()) {
        if (!tile.isPolar() && suitableForNativeSettlement(tile)) {
            for (Tile t : settlementTiles) {
                if (tile.getDistanceTo(t) < minSettlementDistance) {
                    continue tiles;
                }
            }
            settlementTiles.add(tile);
        }
    }
    Collections.shuffle(settlementTiles, random);
    // Check number of settlements.
    int settlementsToPlace = settlementTiles.size();
    float share = settlementsToPlace / shares;
    if (settlementTiles.size() < indians.size()) {
        // TODO: something drastic to boost the settlement number
        logger.warning("There are only " + settlementTiles.size()
                       + " settlement sites."
                       + " This is smaller than " + indians.size()
                       + " the number of tribes.");
    }
    // Find the capitals
    List<Territory> territories
        = new ArrayList<Territory>(territoryMap.values());
    int settlementsPlaced = 0;
    for (Territory territory : territories) {
        switch (((IndianNationType) territory.player.getNationType())
                .getNumberOfSettlements()) {
        case HIGH:
            territory.numberOfSettlements = Math.round(4 * share);
            break;
        case AVERAGE:
            territory.numberOfSettlements = Math.round(3 * share);
            break;
        case LOW:
            territory.numberOfSettlements = Math.round(2 * share);
            break;
        }
        int radius = territory.player.getNationType().getCapitalType().getClaimableRadius();
        ArrayList<Tile> capitalTiles = new ArrayList<Tile>(settlementTiles);
        while (!capitalTiles.isEmpty()) {
            Tile tile = getClosestTile(territory.getCenter(),
                                       capitalTiles);
            capitalTiles.remove(tile);
            // Choose this tile if it is free and half the expected tile
            // claim can succeed (preventing capitals on small islands).
            if (territory.player.getClaimableTiles(tile, radius).size()
                    >= (2 * radius + 1) * (2 * radius + 1) / 2) {
                String name = (territory.region == null) ? "default region"
                              : territory.region.getNameKey();
                logger.fine("Placing the " + territory.player
                            + " capital in region: " + name
                            + " at Tile: "+ tile.getPosition());
                settlements.add(placeIndianSettlement(territory.player,
                                                      true, tile.getPosition(), map));
                territory.numberOfSettlements--;
                territory.position = tile.getPosition();
                settlementTiles.remove(tile);
                settlementsPlaced++;
                break;
            }
        }
    }
    // Sort tiles from the edges of the map inward
    Collections.sort(settlementTiles, new Comparator<Tile>() {
        public int compare(Tile tile1, Tile tile2) {
            int distance1 = Math.min(Math.min(tile1.getX(), map.getWidth() - tile1.getX()),
                                     Math.min(tile1.getY(), map.getHeight() - tile1.getY()));
            int distance2 = Math.min(Math.min(tile2.getX(), map.getWidth() - tile2.getX()),
                                     Math.min(tile2.getY(), map.getHeight() - tile2.getY()));
            return (distance1 - distance2);
        }
    });
    // Now place other settlements
    while (!settlementTiles.isEmpty() && !territories.isEmpty()) {
        Tile tile = settlementTiles.remove(0);
        if (tile.getOwner() != null) continue; // No close overlap
        Territory territory = getClosestTerritory(tile, territories);
        int radius = territory.player.getNationType().getSettlementType(false)
                     .getClaimableRadius();
        // Insist that the settlement can not be linear
        if (territory.player.getClaimableTiles(tile, radius).size()
                > 2 * radius + 1) {
            String name = (territory.region == null) ? "default region"
                          : territory.region.getNameKey();
            logger.fine("Placing a " + territory.player
                        + " camp in region: " + name
                        + " at Tile: " + tile.getPosition());
            settlements.add(placeIndianSettlement(territory.player,
                                                  false, tile.getPosition(), map));
            settlementsPlaced++;
            territory.numberOfSettlements--;
            if (territory.numberOfSettlements <= 0) {
                territories.remove(territory);
            }
        }
    }
    // Grow some more tiles.
    // TODO: move the magic numbers below to the spec RSN
    // Also collect the skills provided
    HashMap<UnitType, List<IndianSettlement>> skills
        = new HashMap<UnitType, List<IndianSettlement>>();
    Collections.shuffle(settlements, random);
    for (IndianSettlement is : settlements) {
        List<Tile> tiles = new ArrayList<Tile>();
        for (Tile tile : is.getOwnedTiles()) {
            for (Tile t : tile.getSurroundingTiles(1)) {
                if (t.getOwningSettlement() == null) {
                    tiles.add(tile);
                    break;
                }
            }
        }
        Collections.shuffle(tiles, random);
        int minGrow = is.getType().getMinimumGrowth();
        int maxGrow = is.getType().getMaximumGrowth();
        if (maxGrow > minGrow) {
            for (int i = random.nextInt(maxGrow - minGrow) + minGrow;
                    i > 0; i--) {
                Tile tile = findFreeNeighbouringTile(is, tiles, random);
                if (tile == null) break;
                tile.changeOwnership(is.getOwner(), is);
                tiles.add(tile);
            }
        }
        // Collect settlements by skill
        UnitType skill = is.getLearnableSkill();
        List<IndianSettlement> isList = skills.get(skill);
        if (isList == null) {
            isList = new ArrayList<IndianSettlement>();
            isList.add(is);
            skills.put(skill, isList);
        } else {
            isList.add(is);
        }
    }
    // Require that there be experts for all the new world goods types.
    // Collect the list of needed experts
    List<UnitType> expertsNeeded = new ArrayList<UnitType>();
    for (GoodsType goodsType : spec.getNewWorldGoodsTypeList()) {
        UnitType expert = spec.getExpertForProducing(goodsType);
        if (!skills.containsKey(expert)) expertsNeeded.add(expert);
    }
    // Extract just the settlement lists.
    List<List<IndianSettlement>> isList
        = new ArrayList<List<IndianSettlement>>(skills.values());
    Comparator<List<IndianSettlement>> listComparator
    = new Comparator<List<IndianSettlement>>() {
        public int compare(List<IndianSettlement> l1,
                           List<IndianSettlement> l2) {
            return l2.size() - l1.size();
        }
    };
    // For each missing skill...
    while (!expertsNeeded.isEmpty()) {
        UnitType neededSkill = expertsNeeded.remove(0);
        Collections.sort(isList, listComparator);
        List<IndianSettlement> extras = isList.remove(0);
        UnitType extraSkill = extras.get(0).getLearnableSkill();
        List<RandomChoice<IndianSettlement>> choices
            = new ArrayList<RandomChoice<IndianSettlement>>();
        // ...look at the settlements with the most common skill
        // with a bit of favoritism to capitals as the needed skill
        // is so rare,...
        for (IndianSettlement is : extras) {
            IndianNationType nation
                = (IndianNationType) is.getOwner().getNationType();
            int cm = (is.isCapital()) ? 2 : 1;
            RandomChoice<IndianSettlement> rc = null;
            for (RandomChoice<UnitType> c : nation.generateSkillsForTile(is.getTile())) {
                if (c.getObject() == neededSkill) {
                    rc = new RandomChoice<IndianSettlement>(is, c.getProbability() * cm);
                    break;
                }
            }
            choices.add((rc != null) ? rc
                        : new RandomChoice<IndianSettlement>(is, 1));
        }
        if (!choices.isEmpty()) {
            // ...and pick one that could do the missing job.
            IndianSettlement chose
                = RandomChoice.getWeightedRandom(logger, "expert", random,
                                                 choices);
            logger.finest("At " + chose.getName()
                          + " replaced " + extraSkill
                          + " (one of " + extras.size() + ")"
                          + " by missing " + neededSkill);
            chose.setLearnableSkill(neededSkill);
            extras.remove(chose);
            isList.add(0, extras); // Try to stay well sorted
            List<IndianSettlement> neededList
                = new ArrayList<IndianSettlement>();
            neededList.add(chose);
            isList.add(neededList);
        } else { // `can not happen'
            logger.finest("Game is missing skill: " + neededSkill);
        }
    }
    String msg = "Settlement skills:";
    for (List<IndianSettlement> iss : isList) {
        if (iss.isEmpty()) {
            msg += "  0 x <none>";
        } else {
            msg += "  " + iss.size() + " x " + iss.get(0).getLearnableSkill();
        }
    }
    logger.info(msg);
    logger.info("Created " + settlementsPlaced
                + " Indian settlements of maximum " + settlementsToPlace);
}