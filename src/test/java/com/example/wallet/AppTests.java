package com.example.wallet;

import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AppTests {

  @Autowired protected MockMvc mockMvc;

  protected String emptyJson() {
    return "{}";
  }

  protected String amountJson(String amount) {
    return "{\"amount\":\"" + amount + "\"}";
  }

  protected String balanceJson(String balance) {
    return "{\"balance\":\"" + balance + "\"}";
  }

  protected void expectBalance(String walletId, String balance) throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + walletId + "/balance"))
        .andExpect(status().isOk())
        .andExpect(content().json(balanceJson(balance), STRICT));
  }
}
