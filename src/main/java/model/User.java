package model;

public class User {
    private String name;
    private String ip;
    private boolean remote = false;

    public User(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public User(String name) {
        this.name = name;
    }

    public User(boolean remote) {
        this.name = "fernbedienung";
        this.remote = true;
    }

    public static User remote() {
        return new User(true);
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

}
