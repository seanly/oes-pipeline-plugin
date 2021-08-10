package cn.k8ops.jenkinsci.plugins.oes;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

public abstract class PipelineConfigProvider
        extends AbstractDescribableImpl<PipelineConfigProvider>
        implements ExtensionPoint {
}
