package net.classicube.launcher;

// Contains information about the updater's state.
// Sent to ClientUpdateScreen from ClientUpdateTask.
public class ClientUpdateStatus implements Cloneable {
    public Op operation;
    public String fileName;
    public int bytesDownloaded;
    public int bytesTotal;
    public int filesProcessed;
    public int filesTotal;
    public int overallProgress;
    
    public static enum Op{
        Downloading, Unpacking
    }
    
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }
}
