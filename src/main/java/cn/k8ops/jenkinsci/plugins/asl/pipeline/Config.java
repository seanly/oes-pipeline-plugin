package cn.k8ops.jenkinsci.plugins.asl.pipeline;

import lombok.Data;
import lombok.SneakyThrows;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Config {

    public static final String KEY_ENVIRONMENT = "environment";
    public static final String KEY_PIPELINE = "pipeline";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_AFTER_STEPS = "after-steps";
    public static final String KEY_NAME = "name";
    public static final String KEY_WHEN = "when";
    public static final String KEY_STEP_ID = "step.id";

    private final Map rawConfig;

    private List<Stage> stages = new ArrayList<>();

    private Map<String, String> environment;

    @SneakyThrows
    Config(String config) {
        this.rawConfig = new Yaml().load(config);
    }

    @SneakyThrows
    public static Config parse(String rawConfig) {
        Config config = new Config(rawConfig);
        config.parseEnvironment();
        config.parsePipeline();
        return config;
    }

    private void parseEnvironment() {
        Map<String, String> localEnviron = new HashMap<>();
        if (rawConfig.containsKey(KEY_ENVIRONMENT)) {
            localEnviron = (Map<String, String>) rawConfig.get(KEY_ENVIRONMENT);
        }

        environment = localEnviron;
    }

    private void parsePipeline() throws ConfigException {
        /**
         * pipeline:
         * - name: build # <- step
         *   steps:
         *     - script:
         *         code: |
         *           echo "hi, ant"
         */

        List pipelineConfig = (List) rawConfig.get(KEY_PIPELINE);

        if (pipelineConfig != null) {
            for (Object stepConfig: pipelineConfig) {
                if (stepConfig instanceof Map) {
                    Stage stage = Stage.parse((Map) stepConfig, environment);
                    stages.add(stage);
                } else {
                    throw new ConfigException("config format error.");
                }
            }
        }
    }

}
