package com.ubs.tariffapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class App {
    private String dbHost;
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
