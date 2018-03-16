package jenkins.plugins.stride;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

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
                String m = message.replaceAll("\"", "\\\"");
                String tmpl = "{\n" +
                        "  \"version\": 1,\n" +
                        "  \"type\": \"doc\",\n" +
                        "  \"content\": [\n" +
                        "    {\n" +
                        "      \"type\": \"panel\",\n" +
                        "      \"attrs\": {\n" +
                        "        \"panelType\": \""+panelType+"\"\n" +
                        "      },\n" +
                        "      \"content\": [\n" +
                        "        {\n" +
                        "          \"type\": \"paragraph\",\n" +
                        "          \"content\": [\n" +
                        "            {\n" +
                        "              \"type\": \"text\",\n" +
                        "              \"text\": \""+m+" \"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"type\": \"text\",\n" +
                        "              \"text\": \"open\",\n" +
                        "              \"marks\": [\n" +
                        "                {\n" +
                        "                  \"type\": \"link\",\n" +
                        "                  \"attrs\": {\n" +
                        "                    \"href\": \""+openURL+"\"\n" +
                        "                  }\n" +
                        "                }\n" +
                        "              ]\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                post.addRequestHeader("Authorization", "Bearer " + token);
                post.addRequestHeader("Content-Type","application/json");
                post.getParams().setContentCharset("UTF-8");
                post.setRequestBody(tmpl);
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

    void setHost(String host) {

    }
}
