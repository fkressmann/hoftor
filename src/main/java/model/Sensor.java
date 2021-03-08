package model;

public class Sensor {
    public Status state;

    public Sensor() {
        this.state = Status.UNKNOWN;
    }


    public void setOpen() {
        state = Status.OPEN;
    }

    public void setClosed() {
        state = Status.CLOSED;
    }



    public boolean isClosed() {
        return state == Status.CLOSED;
    }

    public boolean isOpen() {
        return state == Status.OPEN;
    }

    public boolean isUnknown() {
        return state == Status.UNKNOWN;
    }
}
