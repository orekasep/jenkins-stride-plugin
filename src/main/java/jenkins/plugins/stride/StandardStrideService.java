package jenkins.plugins.stride;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class StandardStrideService implements StrideService {

    private static final Logger logger = Logger.getLogger(StandardStrideService.class.getName());

    private String token;
    private String conversationURL;

    public StandardStrideService(String token, String conversationURL) {
        super();
        this.token = token;
        this.conversationURL = conversationURL;
    }

    public void publish(String message) {
        publish(message, "info", null);
    }

    public void publish(String message, String panelType, String openURL) {
            logger.log(Level.INFO,"Posting: to " + conversationURL + ": " + message + " " + panelType);
            HttpClient client = getHttpClient();
            PostMethod post = new PostMethod(conversationURL);

            try {
                String data = notificationData(message, panelType, openURL);
                post.addRequestHeader("Authorization", "Bearer " + token);
                post.addRequestHeader("Content-Type","application/json");
                post.getParams().setContentCharset("UTF-8");
                post.setRequestEntity(new StringRequestEntity(data, "application/json", "UTF-8"));
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_CREATED ) {
                    logger.log(Level.WARNING, "Stride post may have failed. Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Stride", e);
            } finally {
                post.releaseConnection();
            }
    }

    private String notificationData(String message, String panelType, String openURL) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("version", 1);
        jsonObject.accumulate("type", "doc");
        JSONObject panelContent = new JSONObject();
        panelContent.accumulate("type", "panel");
        JSONObject attrs = new JSONObject();
        attrs.accumulate("panelType", panelType);
        panelContent.accumulate("attrs", attrs);
        JSONObject paragraphContent = new JSONObject();
        paragraphContent.accumulate("type", "paragraph");

        JSONArray messagesContent = new JSONArray();

        JSONObject messageContent = new JSONObject();
        messageContent.accumulate("type", "text");
        messageContent.accumulate("text", message + " ");

        JSONObject linkContent = new JSONObject();
        linkContent.accumulate("type", "text");
        linkContent.accumulate("text", "open");

        JSONArray marks = new JSONArray();
        JSONObject link = new JSONObject();
        link.accumulate("type", "link");
        JSONObject linkAttrs = new JSONObject();
        linkAttrs.accumulate("href", openURL);
        link.accumulate("attrs", linkAttrs);
        marks.add(link);
        linkContent.accumulate("marks", marks);

        messagesContent.add(messageContent);
        messagesContent.add(linkContent);
        paragraphContent.accumulate("content", messagesContent);

        JSONArray paragraphElements = new JSONArray();
        paragraphElements.add(paragraphContent);
        panelContent.accumulate("content", paragraphElements);
        JSONArray panelElements = new JSONArray();
        panelElements.add(panelContent);
        jsonObject.accumulate("content", panelElements);
        return jsonObject.toString();
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            }
        }
        return client;
    }
}
