package cn.opsbox.jenkinsci.plugins.oes.pipeline;

import lombok.Data;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

@Data
public class Step {

    private String id;
    private String version = "";
    private Map<String, String> properties = new HashMap<>();

    /**
     * step name format: xxx/xxx@master
     * @param name
     * @param properties
     */
    public Step(String name, Map<String, String> properties) {
        this(name);
        this.properties = properties;
    }

    public Step(String name) {
        this.version = "";

        String[] stepName = name.split("@");
        if (stepName.length == 2 ) {
            this.id = stepName[0];
            this.version = stepName[1];
        } else {
            this.id = name;
        }
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
