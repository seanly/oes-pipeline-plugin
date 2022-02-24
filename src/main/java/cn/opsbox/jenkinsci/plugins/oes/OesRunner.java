package cn.opsbox.jenkinsci.plugins.oes;

import cn.opsbox.jenkinsci.plugins.oes.pipeline.Config;
import cn.opsbox.jenkinsci.plugins.oes.pipeline.ConfigException;
import cn.opsbox.jenkinsci.plugins.oes.pipeline.Stage;
import cn.opsbox.jenkinsci.plugins.oes.pipeline.Step;
import cn.opsbox.jenkinsci.plugins.oes.registry.RegistryUtil;
import cn.opsbox.jenkinsci.plugins.oes.registry.StepRegistry;
import cn.opsbox.jenkinsci.plugins.oes.util.Constants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class OesRunner extends CLIRunner{

    public static final String DOT_OES_DIR = ".oes";
    public static final String DOT_OES_CI_DIR = ".oes/run";
    public static final String DOT_OES_STEPS_DIR = ".oes/steps";
    public static final String DOT_OES_ENVIRONS_PROPERTIES = ".oes/environs.properties";

    private List<MultiBinding.Unbinder> unbinders = new ArrayList<>();

    private Map<Run<?, ?>, Collection<String>> secretsForBuild = new WeakHashMap<>();

    public OesRunner(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        super(build, launcher, listener);
    }

    public OesRunner(Run<?,?> run, FilePath ws, Launcher launcher, TaskListener listener) {
        super(run, ws, launcher, listener);
    }

    private PrintStream getLogger() {
        return getListener().getLogger();
    }

    @SneakyThrows
    public void createDotOesDir() {
        FilePath dotOesDir = new FilePath(getWs(), DOT_OES_DIR);
        if (!dotOesDir.exists()) {
            dotOesDir.mkdirs();
        }
    }

    @SneakyThrows
    public boolean runPipeline(FilePath pipelineConfigFile, String environs) {

        Config config = Config.parse(pipelineConfigFile.readToString());

        // save .oes/pipeline.final.yml file.
        saveConfig(config);
        saveEnvirons(environs);

        // inject jenkins environs file
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        Map<String, String> paramEnvirons = new HashMap<>();

        if (StringUtils.isNotEmpty(environs)) {
            paramEnvirons = propertiesLoader.getVarsFromPropertiesContent(environs, getEnvvars());
        }

        List<Stage> stages = new ArrayList<>(2);
        if (paramEnvirons.containsKey("_RUN_STAGES")) {
            for (String stageName : StringUtils.split(paramEnvirons.get("_RUN_STAGES"), ",")) {
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

    private void saveConfig(Config config) {
        try {
            Yaml yaml = new Yaml();
            FilePath finalPipelineYmlFile = new FilePath(getWs(), Constants.FINAL_PIPELINE_FILE);
            getLogger().println(String.format("--//save parsed configure file (%s)", finalPipelineYmlFile.getRemote()));
            finalPipelineYmlFile.write(yaml.dumpAsMap(config), "UTF-8");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(getLogger());
        }
    }

    @SneakyThrows
    private void saveEnvirons(String environs) {
        FilePath jenkinsEnvironsFile = new FilePath(getWs(), DOT_OES_ENVIRONS_PROPERTIES);
        FilePath jenkinsEnvironsDir = jenkinsEnvironsFile.getParent();
        if (jenkinsEnvironsDir!= null && !jenkinsEnvironsDir.exists()) {
            jenkinsEnvironsDir.mkdirs();
        }
        jenkinsEnvironsFile.write(getEnvvars().expand(environs), "UTF-8");
    }

    private Stage getStage(List<Stage> stages, String stageName) {
        for (Stage stage: stages) {
            if (stage.getName().equals(stageName)) {
                return stage;
            }
        }

        return null;
    }

    private boolean runStages(List<Stage> stages, Map<String, String> paramEnvirons) {
        boolean ret = false;

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

            for (Map.Entry entry : stageEnvirons.entrySet()) {
                String key = (String) entry.getKey();
                // 使用当前job的env处理environments中的变量
                String value = Util.replaceMacro(String.valueOf(stageEnvirons.get(key)), localEnvvars);
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
        boolean ret = download(step);
        if (ret) {
            return ant(step);
        }
        return false;
    }

    private boolean ant(Step step) {

        String runPropsFileName = String.format("%s-%s.properties", step.getId(), getCurrentTime());

        FilePath dotCIDir = new FilePath(getWs(), DOT_OES_CI_DIR);
        FilePath dotOesStepsDir = new FilePath(getWs(), DOT_OES_STEPS_DIR);

        try {
            Properties stepProps = new Properties();
            for(String key: step.getProperties().keySet()) {
                String value = Util.replaceMacro(step.getProperties().get(key), getEnvvars());
                stepProps.put(key, value);
            }

            String wsDirProp = getWs().getRemote();
            FilePath stepPropsFile = new FilePath(dotCIDir, runPropsFileName);
            FilePath stepPropParent = stepPropsFile.getParent();
            if (stepPropParent != null && !stepPropParent.exists()) {
                stepPropParent.mkdirs();
            }
            stepProps.store(stepPropsFile.write(), "step properties");

            FilePath aslDir = new FilePath(dotOesStepsDir, Constants.STEP_ASL);
            FilePath stepDir = new FilePath(dotOesStepsDir, step.getId());

            FilePath antExecFilePath = new FilePath(aslDir, "tools/ant/bin/ant");
            if (getLauncher().isUnix()) {
                antExecFilePath.chmod(0755);
            } else {
                antExecFilePath = new FilePath(aslDir, "tools/ant/bin/ant.bat");
            }

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(antExecFilePath);
            args.add("-f");
            // run step/run.xml
            args.add(String.format("%s/run.xml", stepDir.getRemote()));
            args.add(String.format("-Dasl.root=%s", aslDir.getRemote()));
            args.add(String.format("-Dws.dir=%s", wsDirProp));
            args.add(String.format("-Dbasedir=%s", wsDirProp));
            // add step runtime arguments
            args.add("-propertyfile");
            args.add(stepPropsFile.getRemote());
            // add run logger.
            args.add("-logger");
            args.add("org.apache.tools.ant.NoBannerLogger");

            return execute(args);

        } catch (IOException e) {
            e.printStackTrace(getLogger());
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            getLogger().println(String.format("secret format error: %s", envKey));
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
        return encoder.encodeToString(originStr.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64(String encodeStr) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(encodeStr), StandardCharsets.UTF_8);
    }

    public boolean download(Step step) {
        PrintStream LOG = getLogger();
        try {
            StepRegistry stepRegistry = RegistryUtil.getStepRegistry();
            String stepId = step.getId();

            FilePath dotOesStepsDir = new FilePath(getWs(), DOT_OES_STEPS_DIR);
            LOG.printf("--//INFO: get step(%s) package...%n", stepId);
            String version = stepRegistry.download(step, dotOesStepsDir);
            LOG.printf("--//INFO: done step (%s:%s).%n", stepId, version);
            FilePath runFilePath = new FilePath(dotOesStepsDir, String.format("%s/run.xml", stepId));

            if (!runFilePath.exists()) {
                LOG.printf("--/ERR: The step type(%s) is not supported.", stepId);
                return false;
            }

            LOG.println("--//INFO: get asl(ant-script-library) package ...");

            Step aslStep = new Step(Constants.STEP_ASL);
            String aslVersion = stepRegistry.download(aslStep, dotOesStepsDir);
            LOG.printf("--//INFO: done asl version: %s.%n", aslVersion);

            return true;

        } catch (Exception e) {
            e.printStackTrace(getLogger());
            return false;
        }
    }
}
