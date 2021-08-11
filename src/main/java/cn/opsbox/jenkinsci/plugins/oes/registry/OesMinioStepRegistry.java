package cn.opsbox.jenkinsci.plugins.oes.registry;

import hudson.FilePath;
import io.minio.*;
import io.minio.messages.Item;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OesMinioStepRegistry extends StepRegistry{

    @Getter
    private String endpoint;

    @Getter
    private String accessKey;

    @Getter
    private String secretKey;

    @Getter
    @Setter
    private String bucket;

    @Getter
    @Setter
    private String archiveLane;

    @Getter
    @Setter
    private String archiveGroup;

    public OesMinioStepRegistry(String endpoint, String accessKey, String secretKey) {
        super();
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    private MinioClient login() {
        return MinioClient.builder().endpoint(this.endpoint)
                .credentials(this.accessKey, this.secretKey).build();
    }

    @Override
    public List<String> getStepList() {

        String groupPath = getGroupPath();
        MinioClient s3Client = login();

        try {
            return getDirList(s3Client, groupPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void download(String version, FilePath saveTo) {

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
