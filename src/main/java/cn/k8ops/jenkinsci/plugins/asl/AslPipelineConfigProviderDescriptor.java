package cn.k8ops.jenkinsci.plugins.asl;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;

public abstract class AslPipelineConfigProviderDescriptor
        extends Descriptor<AslPipelineConfigProvider> {

    @WithBridgeMethods(List.class)
    public static DescriptorExtensionList<AslPipelineConfigProvider, AslPipelineConfigProviderDescriptor> all() {
        return Jenkins.getInstanceOrNull().getDescriptorList(AslPipelineConfigProvider.class);
    }
}
