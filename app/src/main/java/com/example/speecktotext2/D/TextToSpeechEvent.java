package com.example.speecktotext2.D;

public class TextToSpeechEvent {
    private String message;

    public TextToSpeechEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
