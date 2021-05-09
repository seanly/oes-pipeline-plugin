package cn.k8ops.jenkinsci.plugins.asl.workflow;

import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

public abstract class BasicStep extends Step implements Serializable {

    @Getter
    @Setter
    @DataBoundSetter
    protected boolean failOnError = true;
}
