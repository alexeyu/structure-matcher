package nl.alexeyu.structmatcher.examples.bookstore.model;

public final class Server {

    private final String ip;

    private final int port;

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
