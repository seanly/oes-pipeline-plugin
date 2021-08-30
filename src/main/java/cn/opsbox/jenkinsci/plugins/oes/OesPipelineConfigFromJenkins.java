package cn.opsbox.jenkinsci.plugins.oes;

import hudson.Extension;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class OesPipelineConfigFromJenkins extends PipelineConfigProvider {

    @Getter
    private String content;

    @DataBoundConstructor
    public OesPipelineConfigFromJenkins(String content) {
        this.content = content;
    }

    @Symbol("oesPipelineConfigFromJenkins")
    @Extension
    public static final class DescriptorImpl extends PipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline Config";
        }
    }
}
