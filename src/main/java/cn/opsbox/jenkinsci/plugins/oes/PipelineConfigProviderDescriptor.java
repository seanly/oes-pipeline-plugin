package cn.opsbox.jenkinsci.plugins.oes;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;

public abstract class PipelineConfigProviderDescriptor
        extends Descriptor<PipelineConfigProvider> {

    @WithBridgeMethods(List.class)
    public static DescriptorExtensionList<PipelineConfigProvider, PipelineConfigProviderDescriptor> all() {
        return Jenkins.getInstanceOrNull().getDescriptorList(PipelineConfigProvider.class);
    }
}
