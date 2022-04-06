package cn.opsbox.jenkinsci.plugins.oes;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationVersionContributor extends SimpleBuildWrapper implements Serializable {

    public static final String VERSION_PARAMETER = "APP_VERSION";

    @Getter
    private final String versionTemplate;
    @Getter
    private boolean updateDisplayName;

    private static final Logger LOG = Logger.getLogger(ApplicationVersionContributor.class.getName());

    @DataBoundConstructor
    public ApplicationVersionContributor(boolean show, String tpl) {
        this.updateDisplayName = show;
        this.versionTemplate = tpl;
    }

    @Override
    public void setUp(Context context, Run build, FilePath workspace, Launcher launcher, TaskListener listener,
                      EnvVars envVars) throws IOException, InterruptedException {
        try {

            String version = TokenMacro.expandAll(build, workspace, listener, getVersionTemplate());
            setVersion(build, version);
            listener.getLogger().println("Creating version: " + version);

            if (isUpdateDisplayName()) {
                build.setDisplayName(version);
            }

        } catch (MacroEvaluationException e) {
            listener.getLogger().println("Error creating version: " + e.getMessage());
            LOG.log(Level.WARNING, "Error creating version", e);
        }
        String appVersion = getVersion(build);
        if (appVersion != null) {
            context.env(ApplicationVersionContributor.VERSION_PARAMETER, appVersion);
        }
    }

    @CheckForNull
    public static String getVersion(Run<?, ?> build)  {
        ApplicationVersionAction action = build.getAction(ApplicationVersionAction.class);
        if (action != null) {
            return action.getVersion();
        }
        return null;
    }

    static void setVersion(Run<?, ?> build, String version) {
        ApplicationVersionAction action = build.getAction(ApplicationVersionAction.class);
        if (action == null) {
            build.addAction(new ApplicationVersionAction(version));
        } else {
            build.replaceAction(action);
        }
    }

    static class ApplicationVersionAction extends CauseAction {
        private final String version;

        ApplicationVersionAction(final String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }

    @Symbol("withAppVersion")
    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Create App version";
        }
    }


}
