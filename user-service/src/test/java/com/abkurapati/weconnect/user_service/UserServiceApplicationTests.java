package com.abkurapati.weconnect.user_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void contextLoads() {
    }

    private long register(String first, String last) throws Exception {
        String body = """
                {"email":"%s@example.com","password":"password123","firstName":"%s","lastName":"%s","headline":"Engineer"}
                """.formatted(UUID.randomUUID(), first, last);
        String response = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("id").asLong();
    }

    private ResultActions as(long userId, String method, String url, String body) throws Exception {
        var req = method.equals("POST") ? post(url) : delete(url);
        return mvc.perform(req.header("X-User-Id", userId).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : body));
    }

    @Test
    void registrationValidatesAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"firstName\":\"\",\"lastName\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists());

        String dup = "{\"email\":\"dup@example.com\",\"password\":\"password123\",\"firstName\":\"A\",\"lastName\":\"B\"}";
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(dup)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(dup.replace("dup@", "DUP@")))
                .andExpect(status().isConflict());
    }

    @Test
    void profileCrudAndSearch() throws Exception {
        long id = register("Grace", "Hopperson");
        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Grace"));
        mvc.perform(patch("/api/v1/users/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"headline\":\"Admiral\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Admiral"))
                .andExpect(jsonPath("$.firstName").value("Grace"));
        mvc.perform(get("/api/v1/users").param("q", "hopperson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(delete("/api/v1/users/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void connectionWorkflow() throws Exception {
        long alice = register("Alice", "Anders");
        long bob = register("Bob", "Brown");
        long carol = register("Carol", "Clark");

        // alice -> bob
        String created = as(alice, "POST", "/api/v1/connections", "{\"targetUserId\":" + bob + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long connectionId = json.readTree(created).get("id").asLong();

        // duplicates (either direction) and self-connections are refused
        as(alice, "POST", "/api/v1/connections", "{\"targetUserId\":" + bob + "}").andExpect(status().isConflict());
        as(bob, "POST", "/api/v1/connections", "{\"targetUserId\":" + alice + "}").andExpect(status().isConflict());
        as(alice, "POST", "/api/v1/connections", "{\"targetUserId\":" + alice + "}").andExpect(status().isBadRequest());

        // only the receiver may respond
        as(alice, "POST", "/api/v1/connections/" + connectionId + "/accept", null).andExpect(status().isForbidden());
        as(carol, "POST", "/api/v1/connections/" + connectionId + "/accept", null).andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/users/" + bob + "/connections/pending")).andExpect(jsonPath("$.length()").value(1));

        as(bob, "POST", "/api/v1/connections/" + connectionId + "/accept", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        as(bob, "POST", "/api/v1/connections/" + connectionId + "/reject", null).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/users/" + alice + "/connections/count")).andExpect(jsonPath("$.connections").value(1));
        mvc.perform(get("/api/v1/users/" + bob + "/connections")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/users/" + carol + "/connections")).andExpect(jsonPath("$.length()").value(0));

        // a non-participant cannot remove it, a participant can
        as(carol, "DELETE", "/api/v1/connections/" + connectionId, null).andExpect(status().isForbidden());
        as(alice, "DELETE", "/api/v1/connections/" + connectionId, null).andExpect(status().isNoContent());
        assertThat(json.readTree(mvc.perform(get("/api/v1/users/" + alice + "/connections/count")).andReturn().getResponse().getContentAsString())
                .get("connections").asLong()).isZero();
    }

    @Test
    void unknownUsersReturn404() throws Exception {
        long alice = register("Zed", "Zimmer");
        as(alice, "POST", "/api/v1/connections", "{\"targetUserId\":999999}").andExpect(status().isNotFound());
    }
}
