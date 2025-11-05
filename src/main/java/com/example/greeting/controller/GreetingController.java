package com.example.greeting.controller;

import com.example.greeting.dto.Greeting;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    @GetMapping(value = "/greeting", produces = MediaType.APPLICATION_JSON_VALUE)
    public Greeting greeting(@RequestParam(name = "name", defaultValue = "world") String name) {
        if (name == null) {
            throw new IllegalArgumentException("Param 'name' nie może być null");
        }
        // prosty przykład - można tu dodać walidację długości, znaki itp.
        return new Greeting("hello " + name);
    }
}
