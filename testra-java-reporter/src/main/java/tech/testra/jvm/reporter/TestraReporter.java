package tech.testra.jvm.reporter;

import lombok.extern.slf4j.Slf4j;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.EnrichedTestResult;
import tech.testra.jvm.commons.util.PropertyHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static tech.testra.jvm.commons.util.PropertyHelper.prop;

@Slf4j
public abstract class TestraReporter {

    private static final String TESTRA_CONFIG_FILE = ".testra";
    private static final String TESTRA_EXEC_FILE = "testra.exec";
    protected static Map<String, EnrichedTestResult> failedTests;
    protected static TestraTestcaseReporter INSTANCE;
    private static String projectId;
    private static String executionId;

    public TestraReporter() {
        if (!isTestraEnabled()) {
            log.info("Testra is not enabled. Skipping test results reporting to Testra.");
            return;
        }

        loadTestraConfig();
        initialiseProps();
        configureExecution();
    }

    private static void initialiseProps() {
        TestraRestClient.setURLs(prop("apiUrl"));

        projectId = TestraRestClient.getProjectID(prop("project"));
        log.info("Project exists in Testra. Project Id - " + projectId);

        if (prop("buildRef") != null) {
            TestraRestClient.buildRef = prop("buildRef");
        }
        if (prop("exec.desc") != null) {
            TestraRestClient.executionDescription = prop("exec.desc");
        }
    }

    private static void loadTestraConfig() {
        String testraConfigFilePath = "";
        if (Files.exists(Paths.get(TESTRA_CONFIG_FILE))) {
            testraConfigFilePath = TESTRA_CONFIG_FILE;
        } else if (Files.exists(Paths.get("../" + TESTRA_CONFIG_FILE))) {
            testraConfigFilePath = "../" + TESTRA_CONFIG_FILE;
        } else {
            log.error("Testra config file (.testra) is missing in the project root directory");
            log.error("Please disable Testra or place testra config file in the project root directory");
            log.error("Exiting...");
            System.exit(0);
        }
        PropertyHelper.loadPropertiesFromAbsolute(testraConfigFilePath);
    }

    public static boolean isTestraEnabled() {
        return prop("testra") != null;
    }

    public static boolean isRetryMode() {
        return prop("retry") != null;
    }

    private static void configureExecution() {
        if (isRetryMode()) {
            setExecution();
            failedTests =
                    TestraRestClient.getFailedResults().stream()
                            .collect(Collectors.toMap(EnrichedTestResult::getTargetId, r -> r));
        } else {
            createExecution();
        }
        executionId = TestraRestClient.getExecutionid();
        log.info("Execution ID is: " + executionId);
        log.info(prop("apiUrl") + "/projects/" + projectId + "/executions/" + executionId + "\n");
    }

    private static void setExecution() {
        String executionId;
        if (parseBoolean(prop("executionId"))) {
            executionId = prop("executionId");
            log.info("Execution id found in JVM arg. Execution Id - {}", executionId);
        } else {
            executionId = getExecutionIdFromExecFile();
            log.info("Execution id was read from testra.exec file. Execution Id - {}", executionId);
        }
        TestraRestClient.setExecutionid(executionId);
    }

    private static String getExecutionIdFromExecFile() {
        File file = new File(TESTRA_EXEC_FILE);
        if (file.isFile()) {
            try {
                Scanner fileReader = new Scanner(file);
                return fileReader.nextLine();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            throw new RuntimeException("testra.exec file is missing");
        }
    }

    private static void createExecution() {
        TestraRestClient.setExecutionid(null);
        TestraRestClient.createExecutionIDFile();
    }
}
