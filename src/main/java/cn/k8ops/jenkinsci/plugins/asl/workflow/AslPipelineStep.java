package cn.k8ops.jenkinsci.plugins.asl.workflow;

import cn.k8ops.jenkinsci.plugins.asl.*;
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Set;

public class AslPipelineStep extends AslBasicStep {

    public final static String FROM_JENKINS = "jenkins";
    public final static String FROM_WORKSPACE = "workspace";
    public final static String FROM_TEMPLATE = "template";

    /**
     * from: jenkins/workspace/template
     */
    @Getter
    private final String from;

    // template
    @Getter
    @DataBoundSetter
    private String template;

    // jenkins
    @Getter
    @DataBoundSetter
    private  String content;

    // workspace
    @Getter
    @DataBoundSetter
    private String file;

    @Getter
    @DataBoundSetter
    private String environs;

    @DataBoundConstructor
    public AslPipelineStep(String from) {
        this.from = from;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }


    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(
                    Run.class,
                    FilePath.class,
                    Launcher.class,
                    TaskListener.class,
                    EnvVars.class,
                    Computer.class
            );
        }

        @Override
        public String getFunctionName() {
            return "aslPipeline";
        }

        public String getDisplayName() {
            return "Run Asl Pipeline";
        }
    }

    public final class Execution extends SynchronousNonBlockingStepExecution<Void> {

        private final AslPipelineStep step;

        protected Execution(final AslPipelineStep step, final StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {

            Launcher launcher = getContext().get(Launcher.class);
            Run<?, ?> run = getContext().get(Run.class);
            FilePath ws = getContext().get(FilePath.class);
            TaskListener listener = getContext().get(TaskListener.class);
            Computer computer = getContext().get(Computer.class);
            EnvVars envVars = getContext().get(EnvVars.class);

            try {
                AslPipelineConfigProvider provider;
                if (step.from.equals(FROM_JENKINS)) {
                    provider = new AslPipelineConfigFromJenkins(step.content);
                } else if(step.from.equals(FROM_WORKSPACE)) {
                    provider = new AslPipelineConfigFromWorkspace(step.file);
                } else if (step.from.equals(FROM_TEMPLATE)) {
                    provider = new AslPipelineConfigFromTemplate(step.template);
                } else {
                    throw new AbortException("from type is not support");
                }

                AslPipelineBuilder builder = new AslPipelineBuilder(provider, step.environs);
                builder.perform(run, ws, envVars, launcher, listener);

                if (run.getResult() == Result.FAILURE && failOnError) {
                    throw new AbortException("--//ERR: run abs pipeline error");
                }
            } catch (Exception e) {
                if (failOnError) {
                    throw new AbortException(e.getMessage());
                }
            }
            return null;
        }
    }
}
