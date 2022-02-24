package cn.opsbox.jenkinsci.plugins.oes.registry;

import cn.opsbox.jenkinsci.plugins.oes.OesException;
import cn.opsbox.jenkinsci.plugins.oes.pipeline.Step;
import hudson.FilePath;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OesGitlabStepRegistry extends StepRegistry{

    @Getter
    @Setter
    private String gitlabUrl;

    @Getter
    @Setter
    private String accessToken;

    @Getter
    @Setter
    private String stepsGroup;

    private final GitLabApi gitLabApi;

    public OesGitlabStepRegistry(String gitlabUrl, String accessToken) {
        super();
        gitLabApi = new GitLabApi(gitlabUrl, accessToken);
    }

    @SneakyThrows
    @Override
    public List<String> getStepList() {
        List<Project> projects = gitLabApi.getGroupApi().getProjects(this.stepsGroup);
        List<String> steps = new ArrayList<>();

        for (Project project :projects) {
            steps.add(project.getName());
        }

        return steps;
    }

    @SneakyThrows
    @Override
    public String download(Step step, FilePath saveTo) {
        super.download(step, saveTo);

        String stepId = step.getId();
        String stepVersion = step.getVersion();
        Project stepProject = gitLabApi.getProjectApi().getProject(stepsGroup, stepId);
        if (stepVersion.trim().isEmpty()) {
            stepVersion = stepProject.getDefaultBranch();
        }
        FilePath stepDir = new FilePath(saveTo, step.getId());
        stepDir.mkdirs();

        InputStream inputStream = gitLabApi.getRepositoryApi().getRepositoryArchive(stepProject, stepVersion
                , Constants.ArchiveFormat.TAR_GZ);

        FilePath tmp = new FilePath(saveTo, "archive-tmp");
        tmp.mkdirs();
        tmp.untarFrom(inputStream, FilePath.TarCompression.GZIP);
        List<FilePath> dirs = tmp.listDirectories();
        if (dirs.size() == 1) {
            stepDir.deleteRecursive();
            stepDir.mkdirs();
            dirs.get(0).moveAllChildrenTo(stepDir);
        } else {
            throw new OesException("archive package format error");
        }
        // clean tmp dir
        tmp.deleteRecursive();
        return stepVersion;
    }
}
