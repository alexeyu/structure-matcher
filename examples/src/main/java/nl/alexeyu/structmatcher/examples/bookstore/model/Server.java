package nl.alexeyu.structmatcher.examples.bookstore.model;

public final class Server {

    private String ip;

    private int port;

    public Server() {
    }

    public Server(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
