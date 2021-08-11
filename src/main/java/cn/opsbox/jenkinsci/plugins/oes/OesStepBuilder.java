package cn.opsbox.jenkinsci.plugins.oes;

import cn.opsbox.jenkinsci.plugins.oes.pipeline.Step;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OesStepBuilder extends Builder implements SimpleBuildStep {

    public static final String STEPS_DIR = "steps";

    @Getter
    private String stepId;

    @Getter
    private List<OesStepProperty> stepProperties = new ArrayList<>();

    @DataBoundConstructor
    public OesStepBuilder(String stepId) {
        this.stepId = stepId;
    }

    @DataBoundSetter
    public void setStepProperties(List<OesStepProperty> stepProperties) {
        this.stepProperties = stepProperties;
    }
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {

        OesRunner runner = new OesRunner(run, ws, launcher, listener);
        runner.setEnvvars(env);
        runner.copyOesSteps();
        boolean r = runner.runStep(new Step(stepId, convertStepProperties(env)));
        if (r) {
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
            throw new AbortException("--//step build failure...");
        }
    }

    private Map<String, String> convertStepProperties(EnvVars envVars) {
        Map<String, String> retRaw = new HashMap<>();
        for (OesStepProperty var : this.stepProperties) {
            retRaw.put(var.getKey(), envVars.expand(var.getValue()));
        }

        retRaw.put("step.id", stepId);
        return retRaw;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @SneakyThrows
        public ListBoxModel doFillStepIdItems() {

            ListBoxModel items = new ListBoxModel();

            String oesStepsRoot = OesRunner.getOesStepsRoot();
            FilePath rootFilePath = new FilePath(new File(oesStepsRoot));
            FilePath stepsDir = new FilePath(rootFilePath, STEPS_DIR);
            List<FilePath> stepDirs = stepsDir.list();

            for (FilePath stepDir : stepDirs) {
                items.add(stepDir.getName(), stepDir.getName());
            }

            return items;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return "OES Step";
        }
    }
}
