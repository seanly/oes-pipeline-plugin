package cn.opsbox.jenkinsci.plugins.oes;

import hudson.Extension;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class OesPipelineConfigFromWorkspace extends PipelineConfigProvider {

    @Getter
    private String file = DescriptorImpl.DEFAULT_PIPELINE_FILE;

    @DataBoundConstructor
    public OesPipelineConfigFromWorkspace(String file) {
        this.file = file;
    }

    @Symbol("oesPipelineConfigFromWorkspace")
    @Extension
    public static final class DescriptorImpl extends PipelineConfigProviderDescriptor {
        public final static String DEFAULT_PIPELINE_FILE = ".oes/pipeline.yml";

        @Override
        public String getDisplayName() {
            return "Pipeline File";
        }
    }


}
