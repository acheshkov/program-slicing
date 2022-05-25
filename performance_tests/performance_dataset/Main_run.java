private static void run(String[] args) throws Exception {
    PrintUtils.printWithTaskId("Args: " + Arrays.toString(args), PrintUtils.TAG.INFO);
    Options options1 = new Options();
    options1.addOption("l", "lang", true, "c|cpp|java, default java");
    options1.addOption("p", "path", true, "The path to a folder containing the source files");
    options1.addOption("r", "result", true, "Output result file path");
    options1.addOption("t", "type", true, "Defect type. e.g: DataClumps");
    options1.addOption("v", "verbose", false, "Print log details");
    options1.addOption("s", "scope", true, "Scope file path");
    options1.addOption("i", "taskId", true, "task id");
    options1.addOption("m", "mavenRepo", true, "Maven repo path");
    options1.addOption("f", "feThreshold", true, "Feature Envy threshold");
    options1.addOption("g", "gcThreshold", true, "God Class threshold");
    options1.addOption("gr", "godRecommendation", false, "Calculate God Class fix solution");
    options1.addOption("cn", "className", true, "Class qualified name");
    options1.addOption("c", "config", true, "The config.properties path");
    options1.addOption("d", "data", true, "The Bit God Class cache data");
    options1.addOption("lines", "lines", true, "Min Long Method lines of code");
    options1.addOption("sliceTop", "sliceTop", true, "Limit number of the slice");
    options1.addOption("offline", "offline", false,
                       "In the offline mode, the current directory will be used as refactorbot home by default");
    options1.addOption("retrain", "retrain", false, "Retrain");
    options1.addOption("gcLimitMethodNum","gcLimitMethodNum", true, "Useful for God class detection");
    Options options = options1;
    CommandLine cmd = new DefaultParser().parse(options, args, true);
    if (cmd.getOptions().length > 0) {
        GlobalEnvironment.getInstance().init(cmd);
        if (!cmd.hasOption("offline")) {
            ResourceUtils.cleanupExpiredTask();
        }
        FileUtils.mkdirs(GlobalEnvironment.getInstance().currentWorkspace);
        List<BadSmellResult> smellResults = new ArrayList<>();
        if (cmd.hasOption("gr")) {
            String qualifiedNameClass = cmd.getOptionValue("cn");
            Comparator<Integer> comparator = new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    return 0;
                }
            };
            List<BadSmellResult> smellResults1 = new ArrayList<>();
            switch (GlobalEnvironment.getInstance().lang) {
            case JAVA:
                BitGodClassJavaEngine javaEngine = new BitGodClassJavaEngine();
                smellResults1 = javaEngine.calculateFixSolution(qualifiedNameClass);
                break;
            case CPP:
                BitGodClassCxxEngine cxxEngine = new BitGodClassCxxEngine();
                smellResults1 = cxxEngine.calculateFixSolution(qualifiedNameClass);
                break;
            }
            smellResults = smellResults1;
        } else if (cmd.hasOption("retrain")) {
            if (GlobalEnvironment.getInstance().lang == Language.JAVA) {
                Properties config = new Properties();
                try {
                    config.load(new FileReader(GlobalEnvironment.getInstance().configProperties));
                    PrintUtils.printWithTaskId("config.properties = " + config, PrintUtils.TAG.INFO);
                    BitJavaDetector.getBitEngine(config.getProperty("smellType", StringUtils.EMPTY).trim())
                    .ifPresent(engine -> engine.retrainModel(config));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<BadSmellResult> smellResults1 = new ArrayList<>();
            Map<BaseDetector, List<String>> detectorMap = DetectorFactory.getDetector();
            for (Map.Entry<BaseDetector, List<String>> entry : detectorMap.entrySet()) {
                BaseDetector detector = entry.getKey();
                detector.detect(entry.getValue());
                if (!detector.isSuccessful()) {
                    System.err.printf("[Task:%s] %s result not found.%s", GlobalEnvironment.getInstance().taskId,
                                      detector.getClass().getName(), System.lineSeparator());
                    continue;
                }
                List<BadSmellResult> resultList = FilterManager.filter(detector.getBadSmellResult());
                RefactoringEngine refactoringEngine = new RefactoringEngine();
                refactoringEngine.doRefactoring(resultList);
                smellResults1.addAll(resultList);
            }
            smellResults = smellResults1;
        }
        File dir = org.apache.commons.io.FileUtils.getFile(GlobalEnvironment.getInstance().result).getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalArgumentException(GlobalEnvironment.getInstance().result);
        }
        // try-with-resource close writer
        try (FileWriter writer = new FileWriter(GlobalEnvironment.getInstance().result)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            gson.toJson(smellResults, writer);
            writer.flush();
            if (Files.notExists(Paths.get(GlobalEnvironment.getInstance().defaultResult))) {
                org.apache.commons.io.FileUtils
                .copyFile(new File(GlobalEnvironment.getInstance().result),
                          new File(GlobalEnvironment.getInstance().defaultResult));
            }
        }
        if (cmd.hasOption("offline")) {
            ResourceUtils.cleanupCurrentTask();
        }
    } else {
        throw new InvalidOptionsException();
    }
}