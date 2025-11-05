package com.example.greeting.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
public class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void greetsWithName() throws Exception {
        mockMvc.perform(get("/greeting").param("name", "marek"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").value("hello you marek"));
    }

    @Test
    void defaultGreetingWhenNoName() throws Exception {
        mockMvc.perform(get("/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").value("hello you world"));
    }

}
