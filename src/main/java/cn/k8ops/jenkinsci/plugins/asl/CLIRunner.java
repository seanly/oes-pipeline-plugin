package cn.k8ops.jenkinsci.plugins.asl;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks._ant.AntConsoleAnnotator;
import hudson.util.ArgumentListBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.credentialsbinding.masking.SecretPatterns;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLIRunner {

    private final Launcher launcher;
    private final Run<?, ?> build;
    private final TaskListener listener;
    private final FilePath ws;

    @Setter
    @Getter
    private Map<String, String> envvars;

    public Launcher getLauncher() {
        return launcher;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public TaskListener getListener() {
        return listener;
    }

    public FilePath getWs() {
        return ws;
    }

    public CLIRunner(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        this(build, build.getWorkspace(), launcher, listener);
    }

    public CLIRunner(Run<?, ?> build, FilePath ws, Launcher launcher, TaskListener listener) {
        this.launcher = launcher;
        this.build = build;
        this.listener = listener;
        this.ws = ws;
    }

    public boolean execute(ArgumentListBuilder args)
            throws IOException, InterruptedException {
        return this.execute(args, envvars);
    }

    public boolean execute(ArgumentListBuilder args, Map<String, String> env)
            throws IOException, InterruptedException
    {
        if (env == null) {
            env = new HashMap<>();
        }

        if (!launcher.isUnix()) {
            args = toWindowsCommand(args.toWindowsCommand());
        }

        int r = -1;
        try {
            OutputStream out = new Filter(build.getCharset().name(), secretsForBuild).decorateLogger(build, listener.getLogger());
            AntConsoleAnnotator aca = new AntConsoleAnnotator(out, build.getCharset());
            try {
                r = launcher.launch().cmds(args).envs(env).stdout(aca).pwd(ws).join();
            } finally {
                aca.forceEol();
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            throw new AbortException("Exec Failed");
        }

        return r == 0;
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


    @Setter
    private  Map<Run<?, ?>, Collection<String>> secretsForBuild = new WeakHashMap<>();


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
}
