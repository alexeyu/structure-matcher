package nl.alexeyu.structmatcher.examples.bookstore;

public final class Server {

    private String ip;

    private int port;

    Server() {
    }

    Server(String ip, int port) {
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
