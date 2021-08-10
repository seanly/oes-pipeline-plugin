package cn.k8ops.jenkinsci.plugins.asl.registry;

import com.vdurmont.semver4j.Semver;
import hudson.FilePath;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class StepRegistry {

    public abstract List<String> getStepList();
    public abstract void download(String version, FilePath saveTo);

    protected String getLatestVersion(List<String> versions) {

        String latestVersion = StringUtils.EMPTY;

        for (String version : versions) {

            if (StringUtils.isNotEmpty(version)) {

                Semver ver1 = new Semver(latestVersion);
                Semver ver2 = new Semver(version);

                latestVersion = ver1.compareTo(ver2) >= 0 ?
                        ver1.getOriginalValue().trim() :
                        ver2.getOriginalValue().trim();
            }
        }

        return latestVersion;
    }
}
