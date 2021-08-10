package cn.k8ops.jenkinsci.plugins.oes;

import cn.k8ops.jenkinsci.plugins.oes.pipeline.ConfigException;

public class OesException extends Exception {

    public OesException() {
    }

    public OesException(String message) {
        super(message);
    }

    public OesException(Throwable cause) {
        super(cause);
    }

    public OesException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void throwIf(boolean truthTest, String message) throws ConfigException {
        if (truthTest)
            throw new ConfigException(message);
    }

    public void throwIf(boolean truthTest) throws OesException {
        if (truthTest)
            throw this;
    }
}
