package cn.k8ops.jenkinsci.plugins.oes.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension(ordinal = 6)
public class OesGlobalConfiguration extends GlobalConfiguration {

    @Getter
    @Setter
    private StepRegistryProvider stepRegistryProvider;

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public static OesGlobalConfiguration get() {
        return GlobalConfiguration.all().get(OesGlobalConfiguration.class);
    }
}
