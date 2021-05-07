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

import static cn.k8ops.jenkinsci.plugins.asl.AslRunner.DOT_CI_DIR;
import static cn.k8ops.jenkinsci.plugins.asl.AslRunner.JENKINS_DOT_PROPS;

public class AslPipelineBuilder extends Builder implements SimpleBuildStep {

    @Getter
    private String pipelineConfig;

    @Getter
    private String properties;

    @DataBoundConstructor
    public AslPipelineBuilder(String pipelineConfig, String properties) {
        this.pipelineConfig = pipelineConfig;
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
        String ymlPath = ".ant.yml";

        FilePath pipelineConfigFile = new FilePath(ws, ymlPath);
        if (StringUtils.isNotBlank(pipelineConfig)) {
            logger.println("--// start save pipeline config to file");

            if (!pipelineConfigFile.getParent().exists()) {
                pipelineConfigFile.mkdirs();
            }

            pipelineConfigFile.write(pipelineConfig, "UTF-8");
            logger.println("--// save ok");
        } else {
            throw new AbortException("pipeline configure is empty");
        }

        logger.println("--// save jenkins.properties file");

        FilePath jenkinsPropsFile = new FilePath(ws, DOT_CI_DIR + File.separator + JENKINS_DOT_PROPS);

        if(!jenkinsPropsFile.getParent().exists()) {
            jenkinsPropsFile.getParent().mkdirs();
        }
        jenkinsPropsFile.write(env.expand(properties), "UTF-8");
        logger.println("--// save ok");

        logger.println("--// run asl pipeline...");

        AslRunner runner = new AslRunner(run,ws, launcher, listener);
        runner.setEnvvars(env);
        boolean r = runner.runPipeline(pipelineConfigFile, jenkinsPropsFile);

        if (r) {
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
            throw new AbortException("--//pipeline build fail.");
        }
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
