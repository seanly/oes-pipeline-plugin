package cn.k8ops.jenkinsci.plugins.asl;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

public abstract class AslPipelineConfigProvider
        extends AbstractDescribableImpl<AslPipelineConfigProvider>
        implements ExtensionPoint {
}
