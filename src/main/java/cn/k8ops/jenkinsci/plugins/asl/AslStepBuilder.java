package cn.k8ops.jenkinsci.plugins.asl;

import cn.k8ops.jenkinsci.plugins.asl.pipeline.Step;
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

public class AslStepBuilder extends Builder implements SimpleBuildStep {

    public static final String STEPS_DIR = "steps";

    @Getter
    private String stepId;

    @Getter
    private List<AslStepProperty> stepProperties = new ArrayList<>();

    @DataBoundConstructor
    public AslStepBuilder(String stepId) {
        this.stepId = stepId;
    }

    @DataBoundSetter
    public void setStepProperties(List<AslStepProperty> stepProperties) {
        this.stepProperties = stepProperties;
    }
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {

        AslRunner runner = new AslRunner(run, ws, launcher, listener);
        runner.setEnvvars(env);
        runner.copyAntAsl();
        boolean r = runner.runStep(new Step(stepId, convertStepProperties(env)));
        if (r) {
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
            throw new AbortException("--//task build failure...");
        }
    }

    private Map<String, String> convertStepProperties(EnvVars envVars) {
        Map<String, String> retRaw = new HashMap<>();
        for (AslStepProperty var : this.stepProperties) {
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

            String aslRoot = AslRunner.getAslRoot();
            FilePath aslRootFile = new FilePath(new File(aslRoot));
            FilePath stepsDir = new FilePath(aslRootFile, STEPS_DIR);
            List<FilePath> stepDirs = stepsDir.list();

            for (FilePath stepDir : stepDirs) {
                items.add(stepDir.getName(), stepDir.getName());
            }

            return items;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return "ASL Step";
        }
    }
}
