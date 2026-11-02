package com.cloud.engine.pvm.process;

import java.io.Serializable;

public class ParticipantProcess implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
