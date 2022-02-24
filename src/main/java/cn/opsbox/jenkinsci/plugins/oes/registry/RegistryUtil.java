package cn.opsbox.jenkinsci.plugins.oes.registry;

import cn.opsbox.jenkinsci.plugins.oes.OesException;
import cn.opsbox.jenkinsci.plugins.oes.config.OesGitlabStepRegistryProvider;
import cn.opsbox.jenkinsci.plugins.oes.config.OesGlobalConfiguration;
import cn.opsbox.jenkinsci.plugins.oes.config.OesMinioStepRegistryProvider;
import cn.opsbox.jenkinsci.plugins.oes.config.StepRegistryProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public class RegistryUtil {

    public static StepRegistry getStepRegistry() throws OesException {

        StepRegistry stepRegistry;
        StepRegistryProvider stepRegistryProvider = OesGlobalConfiguration.get().getStepRegistryProvider();

        if (stepRegistryProvider instanceof OesMinioStepRegistryProvider) {
            OesMinioStepRegistryProvider provider = (OesMinioStepRegistryProvider) stepRegistryProvider;

            StandardCredentials credentials = provider.getCredentials();

            OesMinioStepRegistry registry = new OesMinioStepRegistry(
                    provider.getEndpoint(),
                    ((StandardUsernamePasswordCredentials) credentials).getUsername(),
                    ((StandardUsernamePasswordCredentials) credentials).getPassword().getPlainText()
            );

            registry.setBucket(provider.getBucket());
            registry.setArchiveLane(provider.getArchiveLane());
            registry.setArchiveGroup(provider.getArchiveGroup());

            stepRegistry = registry;
        } else if (stepRegistryProvider instanceof OesGitlabStepRegistryProvider) {
            OesGitlabStepRegistryProvider provider = (OesGitlabStepRegistryProvider) stepRegistryProvider;

            StandardCredentials credentials = provider.getCredentials();

            OesGitlabStepRegistry registry = new OesGitlabStepRegistry(
                    provider.getGitlabUrl(),
                    ((StandardUsernamePasswordCredentials)credentials).getPassword().getPlainText()
            );

            registry.setStepsGroup(provider.getStepsGroup());

            stepRegistry = registry;
        } else {
            throw new OesException("step registry configure error");
        }

        return stepRegistry;
    }
}
