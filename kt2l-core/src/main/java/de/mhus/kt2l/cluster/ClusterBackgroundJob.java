package de.mhus.kt2l.cluster;

import de.mhus.kt2l.ui.MainView;

import java.io.IOException;

public abstract class ClusterBackgroundJob {

    public abstract void close();

    public abstract void init(MainView mainView, String clusterId, String jobId) throws IOException;

}
