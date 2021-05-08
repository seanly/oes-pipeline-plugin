package cn.k8ops.jenkinsci.plugins.asl;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static cn.k8ops.jenkinsci.plugins.asl.AslPipelineConfigFromWorkspace.DEFAULT_PIPELINE_FILE;
import static cn.k8ops.jenkinsci.plugins.asl.AslRunner.DOT_CI_DIR;
import static cn.k8ops.jenkinsci.plugins.asl.AslRunner.JENKINS_DOT_PROPS;

public class AslPipelineBuilder extends Builder implements SimpleBuildStep {

    @Getter
    private AslPipelineConfigProvider provider;

    @Getter
    private String properties;

    @DataBoundConstructor
    public AslPipelineBuilder(AslPipelineConfigProvider provider, String properties) {
        this.provider = provider;
        this.properties = properties;
    }

    @DataBoundSetter
    public void setProperties(String properties) {
        this.properties = Util.fixEmptyAndTrim(properties);
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();

        String pipelineConfigPath = DEFAULT_PIPELINE_FILE;

        String content;

        if (provider instanceof AslPipelineConfigFromJenkins) {
            AslPipelineConfigFromJenkins fromJenkins = (AslPipelineConfigFromJenkins) provider;
            content = fromJenkins.getContent();
        } else if (provider instanceof AslPipelineConfigFromTemplate) {
            AslPipelineConfigFromTemplate fromTemplate = (AslPipelineConfigFromTemplate) provider;
            content = fromTemplate.getContent();
        } else if (provider instanceof AslPipelineConfigFromWorkspace) {
            AslPipelineConfigFromWorkspace fromWs = (AslPipelineConfigFromWorkspace) provider;
            FilePath configFile = new FilePath(ws, fromWs.getFile());
            content = configFile.readToString();
        } else {
            throw new AbortException("configure provider is not support");
        }
        FilePath configFile = new FilePath(ws, pipelineConfigPath);
        if (!configFile.getParent().exists()) {
            configFile.getParent().mkdirs();
        }

        if (StringUtils.isNotBlank(content)) {
            configFile.write(content, "UTF-8");
        } else {
            throw new AbortException("configure is empty");
        }

        FilePath jenkinsPropsFile = new FilePath(ws, DOT_CI_DIR + File.separator + JENKINS_DOT_PROPS);

        if (!jenkinsPropsFile.getParent().exists()) {
            jenkinsPropsFile.getParent().mkdirs();
        }
        jenkinsPropsFile.write(env.expand(properties), "UTF-8");

        logger.println("--// run asl pipeline...");

        AslRunner runner = new AslRunner(run, ws, launcher, listener);
        runner.setEnvvars(env);
        boolean r = runner.runPipeline(new FilePath(ws, pipelineConfigPath), jenkinsPropsFile);

        if (r) {
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
            throw new AbortException("--//pipeline build fail.");
        }
    }

    @Override
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
