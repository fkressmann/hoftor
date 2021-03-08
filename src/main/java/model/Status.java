package model;

public enum Status {
    OPEN,
    CLOSED,
    OPENING,
    CLOSING,
    UNKNOWN;

    public static Status parse(String in) {
        return Status.valueOf(in.toUpperCase());
    }
}
