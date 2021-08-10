package cn.k8ops.jenkinsci.plugins.oes;

import hudson.Extension;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class OesPipelineConfigFromWorkspace extends PipelineConfigProvider {

    public final static String DEFAULT_PIPELINE_FILE = ".ant.yml";

    @Getter
    private String file = DEFAULT_PIPELINE_FILE;


    @DataBoundConstructor
    public OesPipelineConfigFromWorkspace(String file) {
        this.file = file;
    }

    @Extension
    public static final class DescriptorImpl extends PipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline File";
        }
    }


}
