package cn.opsbox.jenkinsci.plugins.oes.registry;

import com.github.zafarkhaja.semver.Version;
import hudson.FilePath;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

public abstract class StepRegistry {

    public abstract List<String> getStepList();

    @SneakyThrows
    public String download(String stepId, String version, FilePath saveTo){
        this.checkSaveToDir(saveTo);
        return version;
    }

    @SneakyThrows
    public String download(String stepId, FilePath saveTo){
        this.checkSaveToDir(saveTo);
        return null;
    }

    @SneakyThrows
    private void checkSaveToDir(FilePath saveTo){
        if (!saveTo.exists()) {
            saveTo.mkdirs();
        }

        if (!saveTo.isDirectory()) {
            throw new IOException(String.format("dir(%s) is exists, and not dir", saveTo));
        }
    }

    protected String getLatestVersion(List<String> versions) {

        if (versions.size() == 1) {
            return versions.get(0);
        }

        String latestVersion = StringUtils.EMPTY;

        for (String version : versions) {

            if (StringUtils.isEmpty(latestVersion)) {
                latestVersion = version;
                continue;
            }

            if (StringUtils.isNotEmpty(version)) {
                Version ver1 = Version.valueOf(latestVersion);
                Version ver2 = Version.valueOf(version);

                latestVersion = ver1.compareTo(ver2) >= 0 ? latestVersion : version;
            }
        }

        return latestVersion;
    }
}
