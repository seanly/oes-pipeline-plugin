package cn.k8ops.jenkinsci.plugins.asl;

import cn.k8ops.jenkinsci.plugins.asl.pipeline.ConfigException;

public class AslException extends Exception {

    public AslException() {
    }

    public AslException(String message) {
        super(message);
    }

    public AslException(Throwable cause) {
        super(cause);
    }

    public AslException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void throwIf(boolean truthTest, String message) throws ConfigException {
        if (truthTest)
            throw new ConfigException(message);
    }

    public void throwIf(boolean truthTest) throws AslException {
        if (truthTest)
            throw this;
    }
}
