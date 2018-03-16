package jenkins.plugins.stride;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class StrideNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(StrideNotifier.class.getName());

    private String authToken;
    private String buildServerUrl;
    private String room;
    private String sendAs;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getRoom() {
        return room;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getBuildServerUrl() {
        return buildServerUrl;
    }


    @DataBoundConstructor
    public StrideNotifier(final String authToken, final String room, String buildServerUrl) {
        super();
        this.authToken = authToken;
        this.buildServerUrl = buildServerUrl;
        this.room = room;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public StrideService newStrideService(final String token, final String room) {
        return new StandardStrideService(token == null ? getAuthToken(): token, room == null ? getRoom() : room);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }
    public static final String STRIDE_TOKEN = "strideToken";
    public static final String STRIDE_CONVERSATION_URL = "strideConversationURL";
    public static final String STRIDE_BUILD_SERVER_URL = "strideBuildServerUrl";

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String token;
        private String conversationURL;
        private String buildServerUrl;

        public DescriptorImpl() {
            load();
        }

        public String getToken() {
            return token;
        }

        public String getConversationURL() {
            return conversationURL;
        }

        public String getBuildServerUrl() {
            return buildServerUrl;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public StrideNotifier newInstance(StaplerRequest sr, JSONObject data) {
            if (token == null) token = sr.getParameter(STRIDE_TOKEN);
            if (buildServerUrl == null) buildServerUrl = sr.getParameter(STRIDE_BUILD_SERVER_URL);
            if (conversationURL == null) conversationURL = sr.getParameter(STRIDE_CONVERSATION_URL);
            return new StrideNotifier(token, conversationURL, buildServerUrl);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            token = sr.getParameter(STRIDE_TOKEN);
            conversationURL = sr.getParameter(STRIDE_CONVERSATION_URL);
            buildServerUrl = sr.getParameter(STRIDE_BUILD_SERVER_URL);
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            try {
                new StrideNotifier(token, conversationURL, buildServerUrl);
            } catch (Exception e) {
                throw new FormException("Failed to initialize notifier - check your global notifier configuration settings", e, "");
            }
            save();
            return super.configure(sr, formData);
        }

        @Override
        public String getDisplayName() {
            return "Stride Notifications";
        }
    }

    public static class StrideJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {
        private String conversationURL;
        private String token;
        private boolean startNotification;
        private boolean notifySuccess;
        private boolean notifyAborted;
        private boolean notifyNotBuilt;
        private boolean notifyUnstable;
        private boolean notifyFailure;
        private boolean notifyBackToNormal;


        @DataBoundConstructor
        public StrideJobProperty(String conversationURL,
                                 String token,
                                 boolean startNotification,
                                 boolean notifyAborted,
                                 boolean notifyFailure,
                                 boolean notifyNotBuilt,
                                 boolean notifySuccess,
                                 boolean notifyUnstable,
                                 boolean notifyBackToNormal) {
            this.conversationURL = conversationURL;
            this.token = token;
            this.startNotification = startNotification;
            this.notifyAborted = notifyAborted;
            this.notifyFailure = notifyFailure;
            this.notifyNotBuilt = notifyNotBuilt;
            this.notifySuccess = notifySuccess;
            this.notifyUnstable = notifyUnstable;
            this.notifyBackToNormal = notifyBackToNormal;
        }

        @Exported
        public String getConversationURL() {
            return conversationURL;
        }

        @Exported
        public String getToken() {
            return token;
        }

        @Exported
        public boolean getStartNotification() {
            return startNotification;
        }

        @Exported
        public boolean getNotifySuccess() {
            return notifySuccess;
        }

        @Override
        public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            if (startNotification) {
                Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
                for (Publisher publisher : map.values()) {
                    if (publisher instanceof StrideNotifier) {
                        logger.info("Invoking Started...");
                        new ActiveNotifier((StrideNotifier) publisher).started(build);
                    }
                }
            }
            return super.prebuild(build, listener);
        }

        @Exported
        public boolean getNotifyAborted() {
            return notifyAborted;
        }

        @Exported
        public boolean getNotifyFailure() {
            return notifyFailure;
        }

        @Exported
        public boolean getNotifyNotBuilt() {
            return notifyNotBuilt;
        }

        @Exported
        public boolean getNotifyUnstable() {
            return notifyUnstable;
        }

        @Exported
        public boolean getNotifyBackToNormal() {
            return notifyBackToNormal;
        }

        @Extension
        public static final class DescriptorImpl extends JobPropertyDescriptor {
            public String getDisplayName() {
                return "Stride Notifications";
            }

            @Override
            public boolean isApplicable(Class<? extends Job> jobType) {
                return true;
            }

            @Override
            public StrideJobProperty newInstance(StaplerRequest sr, JSONObject formData) throws hudson.model.Descriptor.FormException {
                return new StrideJobProperty(sr.getParameter(STRIDE_CONVERSATION_URL),
                        sr.getParameter(STRIDE_TOKEN),
                        sr.getParameter("strideStartNotification") != null,
                        sr.getParameter("strideNotifyAborted") != null,
                        sr.getParameter("strideNotifyFailure") != null,
                        sr.getParameter("strideNotifyNotBuilt") != null,
                        sr.getParameter("strideNotifySuccess") != null,
                        sr.getParameter("strideNotifyUnstable") != null,
                        sr.getParameter("strideNotifyBackToNormal") != null);
            }
        }
    }
}
