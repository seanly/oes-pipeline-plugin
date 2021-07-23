package cn.k8ops.jenkinsci.plugins.asl;

import hudson.Extension;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class AslPipelineConfigFromJenkins extends AslPipelineConfigProvider {

    @Getter
    private String content;

    @DataBoundConstructor
    public AslPipelineConfigFromJenkins(String content) {
        this.content = content;
    }

    @Extension
    public static final class DescriptorImpl extends AslPipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline Config";
        }
    }
}
