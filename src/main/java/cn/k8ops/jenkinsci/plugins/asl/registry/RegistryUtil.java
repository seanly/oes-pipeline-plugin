package cn.k8ops.jenkinsci.plugins.asl.registry;

import cn.k8ops.jenkinsci.plugins.asl.AslException;
import cn.k8ops.jenkinsci.plugins.asl.config.AslGlobalConfiguration;
import cn.k8ops.jenkinsci.plugins.asl.config.AslMinioStepRegistryProvider;
import cn.k8ops.jenkinsci.plugins.asl.config.StepRegistryProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public class RegistryUtil {

    public static StepRegistry getStepRegistry() throws AslException {

        StepRegistry stepRegistry;
        StepRegistryProvider stepRegistryProvider = AslGlobalConfiguration.get().getStepRegistryProvider();

        if (stepRegistryProvider instanceof AslMinioStepRegistryProvider) {
            AslMinioStepRegistryProvider minioProvider = (AslMinioStepRegistryProvider) stepRegistryProvider;

            StandardCredentials credentials = minioProvider.getCredentials();

            AslMinioStepRegistry minioStepRegistry = new AslMinioStepRegistry(
                    minioProvider.getEndpoint(),
                    ((StandardUsernamePasswordCredentials)credentials).getUsername(),
                    ((StandardUsernamePasswordCredentials)credentials).getPassword().getPlainText()
            );

            minioStepRegistry.setBucket(minioProvider.getBucket());
            minioStepRegistry.setArchiveLane(minioProvider.getArchiveLane());
            minioStepRegistry.setArchiveGroup(minioProvider.getArchiveGroup());

            stepRegistry = minioStepRegistry;
        } else {
            throw new AslException("step registry configure error");
        }

        return stepRegistry;
    }
}
