package com.example.wallet;

import static com.example.wallet.utils.JsonUtils.loadJson;
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

  protected void expectBalance(String walletId, String expectedJson) throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + walletId + "/balance"))
        .andExpect(status().isOk())
        .andExpect(content().json(loadJson(expectedJson), STRICT));
  }
}
