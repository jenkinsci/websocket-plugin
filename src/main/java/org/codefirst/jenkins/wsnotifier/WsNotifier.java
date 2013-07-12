package org.codefirst.jenkins.wsnotifier;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class WsNotifier extends Notifier {

    private static final String START = "START";

    @DataBoundConstructor
    public WsNotifier() {
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        WsServer.send(build);
        return true;
    }

    @Override
    public boolean prebuild(Build build, BuildListener listener) {
        WsServer.send(build, START);
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService(){
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private int port = 8081;
        private boolean useStatusFormat = false;
        private int pingInterval = 20;

        public int port(){ return port; }
        public boolean useStatusFormat(){ return useStatusFormat; }
        public boolean keepalive(){ return pingInterval >= 0; }
        public int pingInterval(){ if (keepalive()) return pingInterval; else return 20; }

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return "Websocket Notifier";
        }

        public FormValidation doCheckPort(@QueryParameter String value)
            throws IOException, ServletException {
            try{
                Integer.parseInt(value);
                return FormValidation.ok();
            }catch(NumberFormatException ex){
                return FormValidation.error("invalid port number");
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            port = formData.getInt("port");
            useStatusFormat = formData.getBoolean("useStatusFormat");
            if (formData.has("keepalive")) {
            	JSONObject keepalive = formData.getJSONObject("keepalive");
                pingInterval = keepalive.getInt("pingInterval");
            } else {
            	pingInterval = -1;
            }
            save();
            WsServer.reset(port, pingInterval, useStatusFormat);
            return super.configure(req,formData);
        }
    }
}
