package cn.k8ops.jenkinsci.plugins.asl.pipeline;

public class ConfigException extends Exception {

    public ConfigException() {
    }

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void throwIf(boolean truthTest, String message) throws ConfigException {
        if (truthTest)
            throw new ConfigException(message);
    }

    public void throwIf(boolean truthTest) throws ConfigException {
        if (truthTest)
            throw this;
    }
}
