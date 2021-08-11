package cn.opsbox.jenkinsci.plugins.oes;

import hudson.Extension;
import hudson.FilePath;
import hudson.util.ListBoxModel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

public class OesPipelineConfigFromTemplate extends PipelineConfigProvider {

    public static final String TEMPLATES_DIR = "templates";

    @Getter
    private String template;

    @DataBoundConstructor
    public OesPipelineConfigFromTemplate(String template) {
        this.template = template;
    }

    @SneakyThrows
    public String getContent() {

        String oesStepsRoot = OesRunner.getOesStepsRoot();
        FilePath rootFilePath = new FilePath(new File(oesStepsRoot));
        FilePath templatesDir = new FilePath(rootFilePath, TEMPLATES_DIR);

        FilePath templateFile = new FilePath(templatesDir, template);

        return templateFile.readToString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends PipelineConfigProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Pipeline Template";
        }

        @SneakyThrows
        public ListBoxModel doFillTemplateItems() {

            ListBoxModel items = new ListBoxModel();

            String oesStepsRoot = OesRunner.getOesStepsRoot();
            FilePath rootFilePath = new FilePath(new File(oesStepsRoot));
            FilePath templatesDir = new FilePath(rootFilePath, TEMPLATES_DIR);
            FilePath[] templates = templatesDir.list("*.yml");

            for (FilePath template : templates) {
                items.add(template.getName(), template.getName());
            }

            return items;
        }
    }
}
