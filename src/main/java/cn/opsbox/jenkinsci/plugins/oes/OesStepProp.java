package cn.opsbox.jenkinsci.plugins.oes;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class OesStepProp extends AbstractDescribableImpl<OesStepProp> {

    @Getter
    private String key;

    @Getter
    private String value;

    @DataBoundConstructor
    public OesStepProp() {}

    @DataBoundSetter
    public void setKey(String key) {
        this.key = key;
    }

    @DataBoundSetter
    public void setValue(String value) {
        this.value = value;
    }

    @Symbol("stepProp")
    @Extension
    public static class DescriptorImpl extends Descriptor<OesStepProp> {

        @Override
        public String getDisplayName() { return ""; }
    }

}
