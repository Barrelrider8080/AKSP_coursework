package org.example.service.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ServiceCommands {
    HELP("/help"),
    INFO("/info"),
    MODELS("/models"),
    SUBSCRIBE("/subscribe"),
    START("/start");

    private final String cmd;

    @Override
    public String toString() {
        return cmd;
    }

    public boolean equals(String cmd) {
        return  this.toString().equals(cmd);
    }
}
