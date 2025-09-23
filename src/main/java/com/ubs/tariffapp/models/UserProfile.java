package com.ubs.tariffapp.models;

import java.util.*;

public class UserProfile {
    private String email;
    private String name;
    private List<String> groups;
    
    public UserProfile(String email, String name, List<String> groups) {
        this.email = email;
        this.name = name;
        this.groups = groups;
    }
}