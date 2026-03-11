package com.core.vdesk.global.oauth2;

import java.util.Map;

public class GoogleUserInfo implements Oauth2UserInfo {

    private Map<String,Object> attribute;

    public GoogleUserInfo(Map<String,Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getId() {
        return attribute.get("sub").toString();
    }

    @Override
    public String getEmail() {
        return attribute.get("email").toString();
    }

    @Override
    public String getName() {
        return attribute.get("name").toString();
    }
}

