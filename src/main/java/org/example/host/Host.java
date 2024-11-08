package org.example.host;

import com.opencsv.bean.CsvBindByName;

public class Host {

    @CsvBindByName
    private String name;

    @CsvBindByName
    private String url;

    @CsvBindByName
    private int heartbeatInterval;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
