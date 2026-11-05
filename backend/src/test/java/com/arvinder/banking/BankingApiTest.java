package com.arvinder.banking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arvinder.banking.domain.Role;
import com.arvinder.banking.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiTest {
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private String register(String username, String phone) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"phone\":\"" + phone + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CUSTOMER"));
        return login(username);
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private long openAccount(String token) throws Exception {
        String body = mvc.perform(post("/api/accounts").header("Authorization", token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.maskedNumber").value(org.hamcrest.Matchers.startsWith("****")))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private JsonNode deposit(String token, long id, String amount, String key) throws Exception {
        var req = post("/api/accounts/" + id + "/deposit").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}");
        if (key != null) {
            req = req.header("Idempotency-Key", key);
        }
        return json.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownUserGiveSameError() throws Exception {
        String user = unique("u");
        register(user, "+1 555 0100");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"wrong-password-1\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("invalid credentials"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody-here\",\"password\":\"wrong-password-1\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("invalid credentials"));
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + unique("u") + "\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void customerCannotReachAdminEndpointsButAdminCan() throws Exception {
        String customer = register(unique("c"), "+1 555 0101");
        mvc.perform(get("/api/admin/users").header("Authorization", customer)).andExpect(status().isForbidden());

        String admin = unique("admin");
        auth.create(admin, PASSWORD, null, Role.ADMIN);
        mvc.perform(get("/api/admin/users").header("Authorization", login(admin))).andExpect(status().isOk());
    }

    @Test
    void transferMovesMoneyAndWritesBothLedgerEntries() throws Exception {
        String alice = register(unique("a"), "+1 555 0102");
        String bob = register(unique("b"), "+1 555 0103");
        long a = openAccount(alice);
        long b = openAccount(bob);
        deposit(alice, a, "100.00", null);

        mvc.perform(post("/api/accounts/transfer").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":" + a + ",\"toAccountId\":" + b + ",\"amount\":40.25}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(59.75));

        mvc.perform(get("/api/accounts").header("Authorization", bob))
            .andExpect(jsonPath("$[0].balance").value(40.25));
        mvc.perform(get("/api/accounts/" + a + "/transactions").header("Authorization", alice))
            .andExpect(jsonPath("$[0].type").value("TRANSFER_OUT"))
            .andExpect(jsonPath("$[0].amount").value(-40.25));
    }

    @Test
    void overdraftIsRejectedAndNothingMoves() throws Exception {
        String alice = register(unique("a"), "+1 555 0104");
        String bob = register(unique("b"), "+1 555 0105");
        long a = openAccount(alice);
        long b = openAccount(bob);
        deposit(alice, a, "10.00", null);
        mvc.perform(post("/api/accounts/transfer").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":" + a + ",\"toAccountId\":" + b + ",\"amount\":10.01}"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/accounts").header("Authorization", bob)).andExpect(jsonPath("$[0].balance").value(0));
    }

    @Test
    void customerCannotTouchSomeoneElsesAccount() throws Exception {
        String alice = register(unique("a"), "+1 555 0106");
        String mallory = register(unique("m"), "+1 555 0107");
        long a = openAccount(alice);
        long m = openAccount(mallory);
        deposit(alice, a, "50.00", null);
        mvc.perform(post("/api/accounts/transfer").header("Authorization", mallory)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":" + a + ",\"toAccountId\":" + m + ",\"amount\":50}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/" + a + "/transactions").header("Authorization", mallory))
            .andExpect(status().isNotFound());
    }

    @Test
    void repeatedIdempotencyKeyIsAppliedOnce() throws Exception {
        String alice = register(unique("a"), "+1 555 0108");
        long a = openAccount(alice);
        String key = UUID.randomUUID().toString();
        deposit(alice, a, "25.00", key);
        JsonNode second = deposit(alice, a, "25.00", key);
        assertThat(second.get("balance").decimalValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void invalidAmountsAreRejected() throws Exception {
        String alice = register(unique("a"), "+1 555 0109");
        long a = openAccount(alice);
        for (String amount : new String[] {"0", "-5", "1.001", "999999"}) {
            mvc.perform(post("/api/accounts/" + a + "/deposit").header("Authorization", alice)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void sensitiveColumnsAreEncryptedAtRest() throws Exception {
        String user = unique("enc");
        String token = register(user, "+1 555 0199");
        openAccount(token);
        String storedPhone = jdbc.queryForObject("select phone from app_users where username = ?", String.class, user);
        String storedNumber = jdbc.queryForObject(
            "select a.account_number from accounts a join app_users u on a.owner_id = u.id where u.username = ?",
            String.class, user);
        String storedHash = jdbc.queryForObject("select password_hash from app_users where username = ?", String.class, user);
        assertThat(storedPhone).doesNotContain("555");
        assertThat(storedNumber).doesNotStartWith("4000");
        assertThat(storedHash).startsWith("$2a$12$");
    }
}
