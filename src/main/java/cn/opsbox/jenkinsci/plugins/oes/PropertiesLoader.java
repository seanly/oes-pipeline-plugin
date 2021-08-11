package cn.opsbox.jenkinsci.plugins.oes;

import hudson.Util;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesLoader implements Serializable {

    @Nonnull
    public Map<String, String> getVarsFromPropertiesContent(@Nonnull String content, @Nonnull Map<String, String> currentEnvVars) throws OesException {
        if (content == null) {
            throw new NullPointerException("A properties content must be set.");
        }
        if (content.trim().length() == 0) {
            throw new IllegalArgumentException("A properties content must be not empty.");
        }

        return getVars(content, currentEnvVars);
    }

    @Nonnull
    private Map<String, String> getVars(@Nonnull String content, @Nonnull Map<String, String> currentEnvVars)
            throws OesException {

        // Replace single backslashes with double ones so they won't be removed by Property.load()
        String escapedContent = content;
        escapedContent = escapedContent.replaceAll("(?<![\\\\])\\\\(?![n:*?\"<>\\\\/])(?![\\\\])(?![\n])", "\\\\\\\\");
        //Escape windows network shares initial double backslash i.e \\Network\Share
        escapedContent = escapedContent.replaceAll("(?m)^([^=]+=)(\\\\\\\\)(?![:*?\"<>\\\\/])", "$1\\\\\\\\\\\\\\\\");

        Map<String, String> result = new LinkedHashMap<>();

        Properties properties = new Properties();

        try (StringReader stringReader = new StringReader(escapedContent)) {
            properties.load(stringReader);
        } catch (IOException ioe) {
            throw new OesException("Problem occurs on loading content", ioe);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(processElement(entry.getKey(), currentEnvVars), processElement(entry.getValue(), currentEnvVars));
        }
        return result;
    }

    @CheckForNull
    private String processElement(@CheckForNull Object prop, @Nonnull Map<String, String> currentEnvVars) {
        String macroProcessedElement = Util.replaceMacro(String.valueOf(prop), currentEnvVars);
        if (macroProcessedElement == null) {
            return null;
        }
        return macroProcessedElement.trim();
    }
}
