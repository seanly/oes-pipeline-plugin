package cn.k8ops.jenkinsci.plugins.asl;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks._ant.AntConsoleAnnotator;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.credentialsbinding.masking.SecretPatterns;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AslPipelineBuilder extends Builder implements SimpleBuildStep {

    @Getter
    private String pipelineConfig;

    @Getter
    private String properties;

    @DataBoundConstructor
    public AslPipelineBuilder(String pipelineConfig, String properties) {
        this.pipelineConfig = pipelineConfig;
        this.properties = properties;
    }

    @DataBoundSetter
    public void setProperties(String properties) {
        this.properties = Util.fixEmptyAndTrim(properties);
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath ws, @NonNull EnvVars env,
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();
        String ymlPath = ".ant.yml";

        FilePath configFile = new FilePath(ws, ymlPath);
        if (StringUtils.isNotBlank(pipelineConfig)) {
            logger.println("--// start save pipeline config to file");

            if (!configFile.getParent().exists()) {
                configFile.mkdirs();
            }

            configFile.write(pipelineConfig, "UTF-8");
            logger.println("--// save ok");
        } else {
            throw new AbortException("pipeline configure is empty");
        }

        logger.println("--// save properties file");
        FilePath propsFile = new FilePath(ws, ".ci/jenkins.properties");
        if(!propsFile.getParent().exists()) {
            propsFile.getParent().mkdirs();
        }
        propsFile.write(env.expand(properties), "UTF-8");
        logger.println("--// save ok");

        logger.println("--// run asl pipeline...");

        FilePath aslDir = copyAntAsl(ws);
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(String.format("%s/tools/ant/bin/ant", aslDir.getRemote()));
        args.add("-f");
        args.add(String.format("%s/run.xml", aslDir.getRemote()));
        args.add("pipeline");
        args.add(String.format("-Dpipeline.file=%s", configFile.getRemote()));
        args.add("-propertyfile");
        args.add(propsFile.getRemote());
        args.add("-logger");
        args.add("org.apache.tools.ant.NoBannerLogger");

        if (!launcher.isUnix()) {
            args = toWindowsCommand(args.toWindowsCommand());
        }

        int r = -1;
        try {
            OutputStream out = new Filter(run.getCharset().name(), new WeakHashMap<>()).decorateLogger(run, listener.getLogger());
            AntConsoleAnnotator aca = new AntConsoleAnnotator(out, run.getCharset());
            try {
                r = launcher.launch().cmds(args).envs(env).stdout(aca).pwd(ws).join();
            } finally {
                aca.forceEol();
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            throw new AbortException("Exec Failed");
        }

        if (r == 0) {
            run.setResult(Result.SUCCESS);
        } else {
            run.setResult(Result.FAILURE);
            throw new AbortException("--//pipeline build fail.");
        }
    }

    /** Similar to {@code MaskPasswordsOutputStream}. */
    private static final class Filter extends ConsoleLogFilter {

        private final String charsetName;

        private Map<Run<?, ?>, Collection<String>> secretsForBuild = new WeakHashMap<Run<?, ?>, Collection<String>>();


        Filter(String charsetName, Map<Run<?, ?>, Collection<String>> secretsForBuild) {
            this.charsetName = charsetName;
            this.secretsForBuild = secretsForBuild;
        }

        /**
         * Gets the {@link Pattern} for the secret values for a given build, if that build has secrets defined. If not, return
         * null.
         * @param build A non-null build.
         * @return A compiled {@link Pattern} from the build's secret values, if the build has any.
         */
        public  @CheckForNull
        Pattern getPatternForBuild(@Nonnull AbstractBuild<?, ?> build) {
            if (secretsForBuild.containsKey(build)) {
                return SecretPatterns.getAggregateSecretPattern(secretsForBuild.get(build));
            } else {
                return null;
            }
        }

        @Override public OutputStream decorateLogger(final AbstractBuild build, final OutputStream logger) throws IOException, InterruptedException {
            return new LineTransformationOutputStream() {
                Pattern p;

                @Override protected void eol(byte[] b, int len) throws IOException {
                    if (p == null) {
                        p = getPatternForBuild(build);
                    }

                    if (p != null && !p.toString().isEmpty()) {
                        Matcher m = p.matcher(new String(b, 0, len, charsetName));
                        if (m.find()) {
                            logger.write(m.replaceAll("****").getBytes(charsetName));
                        } else {
                            // Avoid byte → char → byte conversion unless we are actually doing something.
                            logger.write(b, 0, len);
                        }
                    } else {
                        // Avoid byte → char → byte conversion unless we are actually doing something.
                        logger.write(b, 0, len);
                    }
                }

                @Override public void flush() throws IOException {
                    logger.flush();
                }

                @Override public void close() throws IOException {
                    super.close();
                    logger.close();
                }
            };
        }
    }

    /**
     * Backward compatibility by checking the number of parameters
     *
     */
    public static ArgumentListBuilder toWindowsCommand(ArgumentListBuilder args) {
        List<String> arguments = args.toList();

        if (arguments.size() > 3) { // "cmd.exe", "/C", "ant.bat", ...
            // branch for core equals or greater than 1.654
            boolean[] masks = args.toMaskArray();
            // don't know why are missing single quotes.

            args = new ArgumentListBuilder();
            args.add(arguments.get(0), arguments.get(1)); // "cmd.exe", "/C", ...

            int size = arguments.size();
            for (int i = 2; i < size; i++) {
                String arg = arguments.get(i).replaceAll("^(-D[^\" ]+)=$", "$0\"\"");

                if (masks[i]) {
                    args.addMasked(arg);
                } else {
                    args.add(arg);
                }
            }
        } else {
            // branch for core under 1.653 (backward compatibility)
            // For some reason, ant on windows rejects empty parameters but unix does not.
            // Add quotes for any empty parameter values:
            List<String> newArgs = new ArrayList<String>(args.toList());
            newArgs.set(newArgs.size() - 1, newArgs.get(newArgs.size() - 1).replaceAll(
                    "(?<= )(-D[^\" ]+)= ", "$1=\"\" "));
            args = new ArgumentListBuilder(newArgs.toArray(new String[newArgs.size()]));
        }

        return args;
    }


    @SneakyThrows
    private FilePath copyAntAsl(FilePath ws) {

        String aslRoot = System.getProperty("asl.root");
        if (!StringUtils.isNotBlank(aslRoot)) {
            throw new AbortException("--// 启动的时候没有配置-Dasl.root=/path/to/ant-asl变量");
        }

        File antAslDir = new File(aslRoot);
        FilePath antAslFilePath = new FilePath(antAslDir);
        FilePath dotAslFilePath = new FilePath(ws, ".asl");
        if (!dotAslFilePath.exists()) {
            dotAslFilePath.mkdirs();
        }
        antAslFilePath.copyRecursiveTo(dotAslFilePath);

        return dotAslFilePath;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ASL Pipeline";
        }
    }
}
