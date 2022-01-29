package cn.opsbox.jenkinsci.plugins.oes;

import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static cn.opsbox.jenkinsci.plugins.oes.OesPipelineConfigFromWorkspace.DescriptorImpl.DEFAULT_PIPELINE_FILE;
import static cn.opsbox.jenkinsci.plugins.oes.OesRunner.DOT_OES_CI_DIR;
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
        } else if (provider instanceof OesPipelineConfigFromWorkspace) {
            OesPipelineConfigFromWorkspace fromWs = (OesPipelineConfigFromWorkspace) provider;
            FilePath configFile = new FilePath(ws, fromWs.getFile());
            content = configFile.readToString();
        } else {
            throw new AbortException("configure provider is not support");
        }
        FilePath configFile = new FilePath(ws, pipelineConfigPath);
        FilePath configDir = configFile.getParent();
        if (configDir != null && !configDir.exists()) {
            configDir.mkdirs();
        }

        if (StringUtils.isNotBlank(content)) {
            configFile.write(content, "UTF-8");
        } else {
            throw new AbortException("configure is empty");
        }

        FilePath jenkinsEnvironsFile = new FilePath(ws, DOT_OES_CI_DIR + File.separator + JENKINS_DOT_PROPS);

        FilePath jenkinsEnvironsDir = jenkinsEnvironsFile.getParent();
        if (jenkinsEnvironsDir!= null && !jenkinsEnvironsDir.exists()) {
            jenkinsEnvironsDir.mkdirs();
        }
        logger.println("--// run oes pipeline...");

        // inject jenkins environs file
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        Map<String, String> paramEnvirons = new HashMap<>();

        if (StringUtils.isNotEmpty(environs)) {
            jenkinsEnvironsFile.write(env.expand(environs), "UTF-8");
            paramEnvirons = propertiesLoader.getVarsFromPropertiesContent(environs, env);
        }

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

    @Symbol("oesPipeline")
    @Extension(ordinal = 2)
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
