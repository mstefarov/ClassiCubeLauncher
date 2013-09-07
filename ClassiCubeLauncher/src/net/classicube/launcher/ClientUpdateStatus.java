package net.classicube.launcher;

// Contains information about the updater's state.
// Sent to ClientUpdateScreen from ClientUpdateTask.
public class ClientUpdateStatus implements Cloneable {

    public String fileName;
    public String status;
    public int progress;

    public ClientUpdateStatus(String action, String status, int progress) {
        this.fileName = action;
        this.status = status;
        this.progress = progress;
    }
}
