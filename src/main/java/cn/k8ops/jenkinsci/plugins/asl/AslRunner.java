package cn.k8ops.jenkinsci.plugins.asl;

import cn.k8ops.jenkinsci.plugins.asl.pipeline.Config;
import cn.k8ops.jenkinsci.plugins.asl.pipeline.ConfigException;
import cn.k8ops.jenkinsci.plugins.asl.pipeline.Stage;
import cn.k8ops.jenkinsci.plugins.asl.pipeline.Step;
import hudson.*;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class AslRunner extends CLIRunner{

    public static final String DOT_CI_DIR = ".ci";
    public static final String DOT_ASL_DIR = ".asl";

    public static final String JENKINS_DOT_PROPS = "jenkins.properties";

    private List<MultiBinding.Unbinder> unbinders = new ArrayList<>();

    private Map<Run<?, ?>, Collection<String>> secretsForBuild = new WeakHashMap<>();

    public AslRunner(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        super(build, launcher, listener);
    }

    public AslRunner(Run<?,?> run, FilePath ws, Launcher launcher, TaskListener listener) {
        super(run, ws, launcher, listener);
    }

    private PrintStream getLogger() {
        return getListener().getLogger();
    }

    @SneakyThrows
    public boolean runPipeline(FilePath pipelineConfigFile, Map<String, String> paramEnvirons) {

        Config config = Config.parse(pipelineConfigFile.readToString());

        List<Stage> stages = new ArrayList<>(2);
        if (getEnvvars().containsKey("RUN_STAGES")) {
            for (String stageName : StringUtils.split(getEnvvars().get("RUN_STAGES"), ",")) {
                Stage stage = getStage(config.getStages(), stageName);
                if (stage == null) {
                    throw new ConfigException(String.format("stage(%s) is not configure", stageName));
                }
                stages.add(stage);
            }
        }

        if (stages.size() == 0) {
            stages = config.getStages();
        }

        return runStages(stages, paramEnvirons);
    }

    private Stage getStage(List<Stage> stages, String stageName) {
        for (Stage stage: stages) {
            if (stage.getName().equals(stageName)) {
                return stage;
            }
        }

        return null;
    }

    @SneakyThrows
    public static String getAslRoot() {
        String aslRoot = System.getProperty("asl.root");
        if (!StringUtils.isNotBlank(aslRoot)) {
            throw new AbortException("--// 启动的时候没有配置-Dasl.root=/path/to/ant-asl变量");
        }

        return aslRoot;
    }

    @SneakyThrows
    public void copyAntAsl() {
        getLogger().println("--//copy ant-asl tool....");

        File antAslDir = new File(getAslRoot());
        FilePath antAslFilePath = new FilePath(antAslDir);
        FilePath dotAslFilePath = new FilePath(getWs(), DOT_ASL_DIR);
        if (!dotAslFilePath.exists()) {
            dotAslFilePath.mkdirs();
        }
        antAslFilePath.copyRecursiveTo(dotAslFilePath);
    }

    private boolean runStages(List<Stage> stages, Map<String, String> paramEnvirons) {
        boolean ret = false;
        copyAntAsl();

        for (Stage stage : stages) {
            ret = runStage(stage, paramEnvirons);
            if (!ret) {
                break;
            }
        }

        return ret;
    }

    @SneakyThrows
    private boolean runStage(Stage stage, Map<String, String> paramEnvirons) {

        boolean ret = true;
        try {

            // job envvars data
            EnvVars localEnvvars = getEnvvars();
            // stage envvars data
            Map<String, String> stageEnvirons = stage.getEnvironment();

            // paramEnvirons 覆盖 environments中的环境变量
            stageEnvirons.putAll(paramEnvirons);

            for(String key: stageEnvirons.keySet()) {
                // 使用当前job的env处理environments中的变量
                String value = Util.replaceMacro(String.valueOf(stageEnvirons.get(key)), localEnvvars).trim();
                // 再使用paramEnvirons对value进行二次处理
                value = Util.replaceMacro(value, paramEnvirons);

                // 更新变量的值
                stageEnvirons.put(key, value);
                // 存储变量的值到job的环境变量列表中
                localEnvvars.put(key, value);
            }

            // 处理内部敏感信息变量
            localEnvvars.putAll(bind(stageEnvirons));

            // 设置当前构建任务的变量
            setEnvvars(localEnvvars);

            if (!stage.shouldRun(localEnvvars)) {
                getLogger().println("//INFO: --> skip stage: " + stage.getName());
                return true;
            }

            for (Step step : stage.getSteps()) {
                ret = runStep(step);
                if (!ret) {
                    break;
                }
            }

            if (stage.getAfterSteps().size() != 0) {
                boolean ret2 = runSteps(stage.getAfterSteps());
                if (ret) {
                    ret = ret2;
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace(getLogger());
        } finally {
            // unbind envvars data.
            unbind();
        }
        return ret;
    }

    private boolean runSteps(List<Step> steps) {
        for(Step step : steps) {
            if(!runStep(step)) {
                return false;
            }
        }

        return true;
    }

    public boolean runStep(Step step) {
        return ant(step);
    }

    private boolean ant(Step step) {

        String runPropsFileName = String.format("%s-%s.properties", step.getId(), getCurrentTime());

        FilePath dotCIDir = new FilePath(getWs(), DOT_CI_DIR);
        FilePath dotAslDir = new FilePath(getWs(), DOT_ASL_DIR);

        try {
            Properties stepProps = new Properties();
            for(String key: step.getProperties().keySet()) {
                String value = Util.replaceMacro(step.getProperties().get(key), getEnvvars());
                stepProps.put(key, value);
            }
            stepProps.put("ws.dir", getWs().getRemote());

            FilePath stepPropsFile = new FilePath(dotCIDir, runPropsFileName);
            if (!stepPropsFile.getParent().exists()) {
                stepPropsFile.getParent().mkdirs();
            }
            stepProps.store(stepPropsFile.write(), "step properties");

            if (getLauncher().isUnix()) {
                FilePath antExecFilePath = new FilePath(dotAslDir, "tools/ant/bin/ant");
                antExecFilePath.chmod(0755);
            }

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(String.format("%s/tools/ant/bin/ant", dotAslDir.getRemote()));
            args.add("-f");
            args.add(String.format("%s/run.xml", dotAslDir.getRemote()));
            args.add("step");
            args.add("-propertyfile");
            args.add(stepPropsFile.getRemote());
            args.add("-logger");
            args.add("org.apache.tools.ant.NoBannerLogger");

            return execute(args);

        } catch (Exception e) {
            e.printStackTrace(getLogger());
        }

        return false;
    }


    public MultiBinding getMultiBinding(String envKey, String type, String from) {

        MultiBinding binding = null;

        if (type.equalsIgnoreCase("UsernamePasswordMultiBinding")
                || type.equalsIgnoreCase("usernamePassword")) {
            UsernamePasswordMultiBinding usernamePasswordMultiBinding = new UsernamePasswordMultiBinding(
                    envKey + "_USR", envKey + "_PSW", from);
            binding = usernamePasswordMultiBinding;
        } else if (type.equalsIgnoreCase("UsernamePasswordBinding")
                || type.equalsIgnoreCase("usernameColonPassword")) {
            UsernamePasswordBinding usernamePasswordBinding = new UsernamePasswordBinding(envKey, from);
            binding = usernamePasswordBinding;
        } else if (type.equalsIgnoreCase("FileBinding")
                || type.equalsIgnoreCase("file")) {
            FileBinding fileBinding = new FileBinding(envKey, from);
            binding = fileBinding;
        } else if (type.equalsIgnoreCase("SSHUserPrivateKeyBinding")
                || type.equalsIgnoreCase("sshUserPrivateKey")) {
            SSHUserPrivateKeyBinding sshUserPrivateKeyBinding = new SSHUserPrivateKeyBinding(envKey + "_KEYFILE", from);
            sshUserPrivateKeyBinding.setUsernameVariable(envKey + "_USER");
            sshUserPrivateKeyBinding.setPassphraseVariable(envKey + "_KEYPASSWORD");
            binding = (sshUserPrivateKeyBinding);
        } else if (type.equalsIgnoreCase("StringBinding")
                || type.equalsIgnoreCase("string")) {
            StringBinding stringBinding = new StringBinding(envKey, from);
            binding = (stringBinding);
        }

        return binding;
    }

    public MultiBinding getMultiBinding(String envKey, String secretStr) {

        if (!secretStr.startsWith("secret://jenkins")) {
            return null;
        }

        String secretConfBody = secretStr.replaceFirst("secret://jenkins/", "");

        String[] secretMeta = secretConfBody.split("/", 2);

        if( secretMeta.length != 2) {
            return null;
        }

        String type = secretMeta[0];
        String from = secretMeta[1];

        return getMultiBinding(envKey, type, from);

    }

    public Map<String, String> bind(Map<String, String> envConfig) throws IOException, InterruptedException {
        if (envConfig == null) {
            return new HashMap<>();
        }

        /**
         * envvars format
         * pipeline:
         * - name: build
         *   environment:
         *     ENV_ID: test
         *     BUILD_IMAGE_REGISTRY: secret://jenkins/usernamePassworkd/build-image-registry
         *     KUBECONFIG: secret://jenkins/file/kubeconfig-aliyun-k8s-test
         *
         * # type: usernamePassword/file/sshUserPrivateKey/string/usernameColonPassword
         * # inline: secret://jenkins/type/id
         */

        Map<String, String> env = new HashMap<>();
        List<MultiBinding> bindings = new ArrayList<>();

        for (Map.Entry<String, String> entry : envConfig.entrySet()) {
            String envKey = entry.getKey();

            MultiBinding envvarBinding = getMultiBinding(envKey, entry.getValue());
            if (envvarBinding != null) {
                bindings.add(envvarBinding);
            }
            env.put(envKey, entry.getValue());
        }

        Set<String> secrets = new HashSet<>();

        for (MultiBinding<?> binding : bindings) {
            MultiBinding.MultiEnvironment environment = binding.bind(getBuild(),
                    getWs(), getLauncher(), getListener());
            unbinders.add(environment.getUnbinder());
            env.putAll(environment.getValues());
            secrets.addAll(environment.getValues().values());
        }

        if (!secrets.isEmpty()) {
            secretsForBuild.put(getBuild(), secrets);
        }

        setSecretsForBuild(secretsForBuild);
        return env;
    }

    public void unbind() throws IOException, InterruptedException {
        for (MultiBinding.Unbinder unbinder: unbinders) {
            unbinder.unbind(getBuild(),
                    getWs(), getLauncher(), getListener());
        }
        secretsForBuild.remove(getBuild());
    }


    public static String getCurrentTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
    }

    public static String encodeBase64(String originStr) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(originStr.getBytes());
    }

    public static String decodeBase64(String encodeStr) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(encodeStr));
    }

}
