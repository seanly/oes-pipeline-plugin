package cn.opsbox.jenkinsci.plugins.oes;

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
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import static cn.opsbox.jenkinsci.plugins.oes.OesPipelineConfigFromWorkspace.DEFAULT_PIPELINE_FILE;
import static cn.opsbox.jenkinsci.plugins.oes.OesRunner.DOT_CI_DIR;
import static cn.opsbox.jenkinsci.plugins.oes.OesRunner.JENKINS_DOT_PROPS;

public class OesPipelineBuilder extends Builder implements SimpleBuildStep {

    @Getter
    private PipelineConfigProvider provider;

    @Getter
    private String environs;

    @DataBoundConstructor
    public OesPipelineBuilder(PipelineConfigProvider provider, String environs) {
        this.provider = provider;
        this.environs = environs;
    }

    @DataBoundSetter
    public void setEnvirons(String environs) {
        this.environs = Util.fixEmptyAndTrim(environs);
    }

    @SneakyThrows
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();

        String pipelineConfigPath = DEFAULT_PIPELINE_FILE;

        String content;

        if (provider instanceof OesPipelineConfigFromJenkins) {
            OesPipelineConfigFromJenkins fromJenkins = (OesPipelineConfigFromJenkins) provider;
            content = fromJenkins.getContent();
        } else if (provider instanceof OesPipelineConfigFromTemplate) {
            OesPipelineConfigFromTemplate fromTemplate = (OesPipelineConfigFromTemplate) provider;
            content = fromTemplate.getContent();
        } else if (provider instanceof OesPipelineConfigFromWorkspace) {
            OesPipelineConfigFromWorkspace fromWs = (OesPipelineConfigFromWorkspace) provider;
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

        FilePath jenkinsEnvironsFile = new FilePath(ws, DOT_CI_DIR + File.separator + JENKINS_DOT_PROPS);

        if (!jenkinsEnvironsFile.getParent().exists()) {
            jenkinsEnvironsFile.getParent().mkdirs();
        }
        jenkinsEnvironsFile.write(env.expand(environs), "UTF-8");

        logger.println("--// run oes pipeline...");

        // inject jenkins environs file

        PropertiesLoader propertiesLoader = new PropertiesLoader();
        Map<String, String> paramEnvirons = propertiesLoader.getVarsFromPropertiesContent(environs, env);

        OesRunner runner = new OesRunner(run, ws, launcher, listener);
        runner.setEnvvars(env);
        boolean r = runner.runPipeline(new FilePath(ws, pipelineConfigPath), paramEnvirons);

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
            return "OES Pipeline";
        }
    }
}
