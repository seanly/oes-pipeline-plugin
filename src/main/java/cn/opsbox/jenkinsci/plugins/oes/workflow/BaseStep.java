package cn.opsbox.jenkinsci.plugins.oes.workflow;

import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

public abstract class BaseStep extends Step implements Serializable {

    @Getter
    @Setter
    @DataBoundSetter
    protected boolean failOnError = true;
}