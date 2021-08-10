package cn.k8ops.jenkinsci.plugins.oes.registry;

import cn.k8ops.jenkinsci.plugins.oes.OesException;
import cn.k8ops.jenkinsci.plugins.oes.config.OesGlobalConfiguration;
import cn.k8ops.jenkinsci.plugins.oes.config.OesMinioStepRegistryProvider;
import cn.k8ops.jenkinsci.plugins.oes.config.StepRegistryProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public class RegistryUtil {

    public static StepRegistry getStepRegistry() throws OesException {

        StepRegistry stepRegistry;
        StepRegistryProvider stepRegistryProvider = OesGlobalConfiguration.get().getStepRegistryProvider();

        if (stepRegistryProvider instanceof OesMinioStepRegistryProvider) {
            OesMinioStepRegistryProvider minioProvider = (OesMinioStepRegistryProvider) stepRegistryProvider;

            StandardCredentials credentials = minioProvider.getCredentials();

            OesMinioStepRegistry minioStepRegistry = new OesMinioStepRegistry(
                    minioProvider.getEndpoint(),
                    ((StandardUsernamePasswordCredentials)credentials).getUsername(),
                    ((StandardUsernamePasswordCredentials)credentials).getPassword().getPlainText()
            );

            minioStepRegistry.setBucket(minioProvider.getBucket());
            minioStepRegistry.setArchiveLane(minioProvider.getArchiveLane());
            minioStepRegistry.setArchiveGroup(minioProvider.getArchiveGroup());

            stepRegistry = minioStepRegistry;
        } else {
            throw new OesException("step registry configure error");
        }

        return stepRegistry;
    }
}
