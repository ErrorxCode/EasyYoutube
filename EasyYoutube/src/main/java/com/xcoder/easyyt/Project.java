package com.xcoder.easyyt;

import java.util.ArrayList;
import java.util.List;

public class Project {
    public final String clientId;
    public final String clientSecret;
    public final String refreshToken;

    public Project(String clientId, String clientSecret,String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }
}
