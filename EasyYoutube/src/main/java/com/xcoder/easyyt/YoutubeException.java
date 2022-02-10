package com.xcoder.easyyt;

import java.io.IOException;

public class YoutubeException extends IOException {
    private final String reasons;

    public YoutubeException(String message, String reasons) {
        super(message);
        this.reasons = reasons;
    }

    public String getReasons(){
        return reasons;
    }
}
