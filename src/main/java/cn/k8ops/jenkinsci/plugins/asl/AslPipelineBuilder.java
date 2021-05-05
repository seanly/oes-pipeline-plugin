package cn.k8ops.jenkinsci.plugins.asl;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.PrintStream;

public class AslPipelineBuilder extends Builder implements SimpleBuildStep {

    @Getter
    private String pipelineConfig;

    @Getter
    private String steps;

    @Getter
    private String properties;

    @DataBoundConstructor
    public AslPipelineBuilder(String pipelineConfig, String steps, String properties) {
        this.pipelineConfig = pipelineConfig;
        this.properties = properties;
        this.steps = steps;
    }

    @DataBoundSetter
    public void setProperties(String properties) {
        this.properties = Util.fixEmptyAndTrim(properties);
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) {

        PrintStream logger = listener.getLogger();
        logger.println("hi, jenkins");
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ASL Pipeline";
        }
    }
}
