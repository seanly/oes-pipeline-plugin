package cn.opsbox.jenkinsci.plugins.oes.macro;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * VERSION_FILE macro
 * format: ${VERSION_FILE, path="VERSION"} or ${VERSION_FILE}
 */
@Extension
public class VersionFileMacro extends DataBoundTokenMacro {
    @Parameter(required = false)
    public String path = VERSION_FILE_PATH;

    private static final String VERSION_FILE_PATH = "VERSION";

    private static final String fileNotFoundMessage = "ERROR: File '%s' does not exist";

    private static final ArrayList<String> macroNames = new ArrayList() {{
        add("VERSION");
    }};

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroNames.contains(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return macroNames;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context,getWorkspace(context),listener,macroName);
    }

    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        if(!workspace.child(path).exists()) {
            throw new MacroEvaluationException(String.format(fileNotFoundMessage, path));
        }

        try {
            StringBuilder result = new StringBuilder();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(workspace.child(path).read(), Charset.defaultCharset()));
                // 只读取第一行
                result.append(reader.readLine());
            } finally {
                if(reader != null) {
                    reader.close();
                }
            }

            String version = result.toString().trim();
            if (StringUtils.isEmpty(version)) {
                throw new MacroEvaluationException("version is empty string.");
            }

            return version;
        } catch (IOException e) {
            throw new MacroEvaluationException(String.format("ERROR: File %s could not be read", path));
        }
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
