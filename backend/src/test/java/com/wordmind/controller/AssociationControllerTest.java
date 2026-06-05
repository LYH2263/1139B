package com.wordmind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.AssociationDTO;
import com.wordmind.dto.AuthDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssociationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private Long testWordId = 1L;

    @BeforeEach
    void setUp() throws Exception {
        AuthDTO.LoginRequest userLogin = new AuthDTO.LoginRequest();
        userLogin.setUsername("user");
        userLogin.setPassword("user123");
        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn();
        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    @Test
    void testGetAssociations() throws Exception {
        mockMvc.perform(get("/api/words/" + testWordId + "/associations")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").exists());
    }

    @Test
    void testCreateAssociation() throws Exception {
        AssociationDTO.CreateRequest request = new AssociationDTO.CreateRequest();
        request.setWordId(testWordId);
        request.setType("用户分享");
        request.setContent("这是一个测试联想记忆方法");

        mockMvc.perform(post("/api/words/" + testWordId + "/associations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.type").value("用户分享"))
                .andExpect(jsonPath("$.data.content").value("这是一个测试联想记忆方法"));
    }

    @Test
    void testUpvoteAssociation() throws Exception {
        AssociationDTO.CreateRequest request = new AssociationDTO.CreateRequest();
        request.setWordId(testWordId);
        request.setType("用户分享");
        request.setContent("点赞测试");

        MvcResult createResult = mockMvc.perform(post("/api/words/" + testWordId + "/associations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        Long associationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/api/associations/" + associationId + "/upvote")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.upvotes").value(1));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/words/" + testWordId + "/associations"))
                .andExpect(status().isForbidden());
    }
}
