package cn.k8ops.jenkinsci.plugins.asl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.CauseAction;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationVersionContributor extends BuildWrapper {

    public static final String VERSION_PARAMETER = "APP_VERSION";

    private final String versionTemplate;
    private boolean updateDisplayName;

    private static final Logger LOG = Logger.getLogger(ApplicationVersionContributor.class.getName());

    @DataBoundConstructor
    public ApplicationVersionContributor(boolean updateDisplayName, String versionTemplate) {
        this.updateDisplayName = updateDisplayName;
        this.versionTemplate = versionTemplate;
    }

    public String getVersionTemplate() {
        return versionTemplate;
    }

    public boolean isUpdateDisplayName() {
        return updateDisplayName;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        try {

            String version = TokenMacro.expandAll(build, listener, getVersionTemplate());
            setVersion(build, version);
            listener.getLogger().println("Creating version: " + version);

            if (isUpdateDisplayName()) {
                build.setDisplayName(version);
            }

        } catch (MacroEvaluationException e) {
            listener.getLogger().println("Error creating version: " + e.getMessage());
            LOG.log(Level.WARNING, "Error creating version", e);
        }
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) {
                return true;
            }
        };
    }

    @CheckForNull
    public static String getVersion(AbstractBuild build)  {
        ApplicationVersionAction action = build.getAction(ApplicationVersionAction.class);
        if (action != null) {
            return action.getVersion();
        }
        return null;
    }

    static void setVersion(AbstractBuild build, String version) {
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
