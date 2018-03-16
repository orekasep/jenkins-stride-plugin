package jenkins.plugins.stride;

public interface StrideService {
    void publish(String message);

    void publish(String message, String color, String openURL);
}
