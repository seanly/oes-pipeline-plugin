package cn.k8ops.jenkinsci.plugins.asl.workflow;

import cn.k8ops.jenkinsci.plugins.asl.AslStepBuilder;
import cn.k8ops.jenkinsci.plugins.asl.AslStepProperty;
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.*;
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.*;

public class AslStepStep extends AslBasicStep {

    @Getter
    private final String stepId;

    @Getter
    private Map<String, Object> stepProperties = new HashMap<String, Object>();

    @DataBoundConstructor
    public AslStepStep(String stepId) {
        this.stepId = stepId;
    }

    @DataBoundSetter
    public void setStepProperties(Map<String, Object> stepProperties) {
        this.stepProperties = stepProperties;

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
            return "aslStep";
        }

        @Override
        public String getDisplayName() {
            return getPrefix() + " aslStep";
        }

        protected String getPrefix() {
            return "asl-step: ";
        }
    }

    public final class Execution extends SynchronousStepExecution<Void> {

        private final AslStepStep step;

        protected Execution(final AslStepStep step, final StepContext context) {
            super(context);
            this.step = step;
        }

        private List<AslStepProperty> convertExtraVars(Map<String, Object> extraVars) {
            if (extraVars == null) {
                return null;
            }
            List<AslStepProperty> extraVarList = new ArrayList<>();
            for (Map.Entry<String, Object> entry: extraVars.entrySet()) {
                AslStepProperty var = new AslStepProperty();
                var.setKey(entry.getKey());
                Object o = entry.getValue();
                if (o instanceof Map) {
                    var.setValue(((Map)o).get("value").toString());
                } else {
                    var.setValue(o.toString());
                }
                extraVarList.add(var);
            }
            return extraVarList;
        }

        @Override
        protected Void run() throws Exception {

            Launcher launcher = getContext().get(Launcher.class);
            Run<?, ?> run = getContext().get(Run.class);
            FilePath ws = getContext().get(FilePath.class);
            TaskListener listener = getContext().get(TaskListener.class);
            EnvVars envVars = getContext().get(EnvVars.class);

            AslStepBuilder builder = new AslStepBuilder(step.getStepId());
            builder.setStepProperties(convertExtraVars(step.getStepProperties()));

            try {

                builder.perform(run, ws, envVars, launcher, listener);

                if (run.getResult() == Result.FAILURE && failOnError) {
                    throw new AbortException(String.format("--//ERR: run step error: %s", step.getStepId()));
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
