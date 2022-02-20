package cn.opsbox.jenkinsci.plugins.oes.config;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;

public abstract class StepRegistryProviderDescriptor extends Descriptor<StepRegistryProvider> {

    @WithBridgeMethods(List.class)
    public static DescriptorExtensionList<StepRegistryProvider, StepRegistryProviderDescriptor> all() {

        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance != null) {
            return instance.getDescriptorList(StepRegistryProvider.class);
        } else {
            return null;
        }
    }
}
