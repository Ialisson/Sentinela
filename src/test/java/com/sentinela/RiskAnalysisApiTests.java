package com.sentinela;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RiskAnalysisApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzesTransactionAndReturnsTriggeredRules() throws Exception {
        mockMvc.perform(post("/api/v1/risk/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "TXN001",
                                  "userId": "USER123",
                                  "amount": 9500.00,
                                  "country": "NG",
                                  "ipAddress": "192.168.1.100",
                                  "cardAttempts": 5,
                                  "emailAgeDays": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(100))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.recommendedAction").value("BLOCK"))
                .andExpect(jsonPath("$.triggeredRules.length()").value(4));
    }

    @Test
    void rejectsInvalidRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/risk/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": " ",
                                  "userId": "USER123",
                                  "amount": -1,
                                  "country": "BRA",
                                  "cardAttempts": 0,
                                  "emailAgeDays": -2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.transactionId").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.country").exists())
                .andExpect(jsonPath("$.errors.cardAttempts").exists())
                .andExpect(jsonPath("$.errors.emailAgeDays").exists());
    }

    @Test
    void amountAtThresholdDoesNotTriggerHighAmountRule() throws Exception {
        mockMvc.perform(post("/api/v1/risk/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "TXN002",
                                  "userId": "USER456",
                                  "amount": 5000.00,
                                  "country": "BR",
                                  "cardAttempts": 1,
                                  "emailAgeDays": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(0))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.recommendedAction").value("APPROVE"))
                .andExpect(jsonPath("$.triggeredRules.length()").value(0));
    }
}
