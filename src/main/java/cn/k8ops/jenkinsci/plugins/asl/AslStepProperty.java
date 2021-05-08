package cn.k8ops.jenkinsci.plugins.asl;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class AslStepProperty extends AbstractDescribableImpl<AslStepProperty> {

    @Getter
    private String key;

    @Getter
    private String value;

    @DataBoundConstructor
    public AslStepProperty() {}

    @DataBoundSetter
    public void setKey(String key) {
        this.key = key;
    }

    @DataBoundSetter
    public void setValue(String value) {
        this.value = value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AslStepProperty> {

        @Override
        @NotNull
        public String getDisplayName() { return ""; }
    }

}
