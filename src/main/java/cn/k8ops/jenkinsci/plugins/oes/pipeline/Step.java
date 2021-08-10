package cn.k8ops.jenkinsci.plugins.oes.pipeline;

import lombok.Data;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

@Data
public class Step {

    private String id;
    private Map<String, String> properties = new HashMap<>();

    public Step(String id, Map<String, String> properties) {
        this.id = id;
        this.properties = properties;
    }

    public Step(String id) {
        this.id = id;
    }

    @SneakyThrows
    public static Step parse(Object  rawConfig) {
        Step step = null;
        /**
         * v2 step format
         *
         * - maven
         */
        if (rawConfig instanceof String) {
            step = new Step((String)rawConfig);
        }

        if (rawConfig instanceof Map) {
            Map stepConfig = (Map) rawConfig;

            if (stepConfig.size() == 1) {
                Map.Entry<String, Object> stepEntry = (Map.Entry<String, Object>) stepConfig.entrySet().iterator().next();

                if (stepEntry.getKey().equals(Config.KEY_STEP_ID)) {
                    /**
                     * v2 step format
                     * - step.id: semver
                     */
                    step = new Step((String) stepEntry.getValue());
                } else {
                    /**
                     * v2 step format
                     * - maven:
                     *     goal: clean package
                     *     options: -Dmaven.test.skip=true
                     */
                    if (stepEntry.getValue() instanceof Map) {
                        step = new Step(stepEntry.getKey(), (Map<String, String>) stepEntry.getValue());
                    } else {
                        throw new ConfigException("task configure format error");
                    }
                }
            } else {
                /**
                 * v1 step format
                 * - step.id: maven
                 *   goal: clean package
                 *   options: -Dmaven.test.skip=true
                 */
                if (stepConfig.containsKey(Config.KEY_STEP_ID)) {
                    String id = (String) stepConfig.get(Config.KEY_STEP_ID);
                    step = new Step(id, stepConfig);
                }
            }
        }

        step.properties.put("step.id", step.getId());

        return step;
    }
}
