package cn.k8ops.jenkinsci.plugins.asl;

import hudson.Extension;
import hudson.FilePath;
import hudson.util.ListBoxModel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

public class AslPipelineConfigFromTemplate extends AslPipelineConfigProvider{

    public static final String TEMPLATES_DIR = "templates";

    @Getter
    private String template;

    @DataBoundConstructor
    public AslPipelineConfigFromTemplate(String template) {
        this.template = template;
    }

    @SneakyThrows
    public String getContent() {

        String aslRoot = AslRunner.getAslRoot();
        FilePath aslRootFile = new FilePath(new File(aslRoot));
        FilePath templatesDir = new FilePath(aslRootFile, TEMPLATES_DIR);

        FilePath templateFile = new FilePath(templatesDir, template);

        return templateFile.readToString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends AslPipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline Template";
        }

        @SneakyThrows
        public ListBoxModel doFillTemplateItems() {

            ListBoxModel items = new ListBoxModel();

            String aslRoot = AslRunner.getAslRoot();
            FilePath aslRootFile = new FilePath(new File(aslRoot));
            FilePath templatesDir = new FilePath(aslRootFile, TEMPLATES_DIR);
            FilePath[] templates = templatesDir.list("*.yml");

            for (FilePath template : templates) {
                items.add(template.getName(), template.getName());
            }

            return items;
        }
    }
}
