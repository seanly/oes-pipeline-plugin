package cn.opsbox.jenkinsci.plugins.oes.registry;

import hudson.FilePath;
import io.minio.*;
import io.minio.messages.Item;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OesMinioStepRegistry extends StepRegistry{

    @Getter
    @Setter
    private String bucket;

    @Getter
    @Setter
    private String archiveLane;

    @Getter
    @Setter
    private String archiveGroup;

    private final MinioClient client;

    public OesMinioStepRegistry(String endpoint, String accessKey, String secretKey) {
        super();
        client = MinioClient.builder().endpoint(endpoint)
                .credentials(accessKey, secretKey).build();
    }

    @Override
    public List<String> getStepList() {

        String groupPath = getGroupPath();

        try {
            return getDirList(client, groupPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String download(String stepId, FilePath saveTo) {
        return this.download(stepId,"", saveTo);
    }

    @SneakyThrows
    @Override
    public String download(String stepId, String version, FilePath saveTo) {
        super.download(stepId, saveTo);

        String currentVersion = version;
        if (StringUtils.isEmpty(version)) {
           currentVersion = getStepLatestVersion(stepId);
        }

        String packageFileName = String.format("%s-%s.tar.gz", stepId, currentVersion);
        FilePath packageFilePath = new FilePath(saveTo, packageFileName);
        String packageRemotePath = String.format("%s/%s/%s", getStepPath(stepId), currentVersion, packageFileName);
        FilePath packageMd5FilePath = new FilePath(saveTo, String.format("%s.md5", packageFileName));
        String packageMd5RemotePath = String.format("%s.md5", packageRemotePath);

        packageMd5FilePath.copyFrom(getFileInputStream(client, packageMd5RemotePath));
        String md5Code = packageMd5FilePath.readToString().trim();

        boolean isLatest = isLatestPkg(md5Code, packageFilePath);
        if (!isLatest) {
            // download step package
            packageFilePath.copyFrom(getFileInputStream(client, packageRemotePath));

            String fileMd5code = DigestUtils.md5Hex(packageFilePath.read());
            if (fileMd5code.compareTo(md5Code) != 0) {
                throw new IOException("package verify error");
            }
        }

        FilePath saveToTaskId = new FilePath(saveTo, stepId);
        packageFilePath.untar(saveToTaskId, FilePath.TarCompression.GZIP);
        return currentVersion;
    }

    boolean isLatestPkg(String latestMd5, FilePath pkgFile) throws IOException, InterruptedException {

        if (!pkgFile.exists()) {
            return false;
        }

        String fileMd5code = DigestUtils.md5Hex(pkgFile.read());
        return fileMd5code.compareTo(latestMd5) == 0;
    }

    private InputStream getFileInputStream(MinioClient client, String remoteFilePath) throws IOException {

        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(remoteFilePath).build());
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(remoteFilePath).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("get download input stream error.");
        }
    }

    private String getStepLatestVersion(String stepId) throws IOException{
        String taskPath = getStepPath(stepId);
        List<String> versionDirs = getDirList(client, String.format("%s/", taskPath));
        if (versionDirs.size() == 0) {
            throw new IOException(String.format("don't %s package", stepId));
        }

        return getLatestVersion(versionDirs);
    }

    String getStepPath(String stepId) {
        return String.format("%s%s", getGroupPath(), stepId);
    }

    private String getGroupPath() {
        String groupDir = StringUtils.join(StringUtils.split(archiveGroup, "."), "/");

        String laneDir = StringUtils.EMPTY;
        if (!StringUtils.isEmpty(archiveLane)) {
            laneDir = String.format("/%s", archiveLane);
        }

        return String.format("%s/%s/", laneDir, groupDir);
    }

    private List<String> getDirList(MinioClient client, String parentPath) throws IOException {

        List<String> dirList = new ArrayList<>();
        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                throw new IOException(String.format("bucket(%s) is not exists", bucket));
            }

            Iterable<Result<Item>> myObjects = client.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).prefix(parentPath).build());

            for (Result<Item> result : myObjects) {
                Item item = result.get();
                String versionName = item.objectName().substring(parentPath.length()).split("/", 2)[0];
                dirList.add(versionName);
            }
            return dirList;

        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
