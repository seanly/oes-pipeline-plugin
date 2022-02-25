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

            if (credentials == null) {
                throw new OesException("minio auth configure error");
            }

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
            String accessToken = "";

            if (credentials != null) {
                accessToken = ((StandardUsernamePasswordCredentials)credentials).getPassword().getPlainText();
            }
            OesGitlabStepRegistry registry = new OesGitlabStepRegistry(
                    provider.getGitlabUrl(),
                    accessToken
            );
            registry.setStepsGroup(provider.getStepsGroup());
            stepRegistry = registry;
        } else {
            throw new OesException("step registry configure error");
        }

        return stepRegistry;
    }
}
