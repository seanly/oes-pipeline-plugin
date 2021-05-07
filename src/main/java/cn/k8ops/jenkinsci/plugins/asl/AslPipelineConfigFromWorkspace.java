package cn.k8ops.jenkinsci.plugins.asl;

import hudson.Extension;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class AslPipelineConfigFromWorkspace extends AslPipelineConfigProvider {

    public final static String DEFAULT_PIPELINE_FILE = ".ant.yml";

    @Getter
    private String file = DEFAULT_PIPELINE_FILE;


    @DataBoundConstructor
    public AslPipelineConfigFromWorkspace(String file) {
        this.file = file;
    }

    @Extension
    public static final class DescriptorImpl extends AslPipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline file";
        }
    }


}
