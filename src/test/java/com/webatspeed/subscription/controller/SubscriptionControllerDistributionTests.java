package com.webatspeed.subscription.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.webatspeed.subscription.service.Subscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerDistributionTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private Subscriber subscriber;

  @Test
  void applySubscriptionsShouldRespondWithAcceptedIfNotDistributing() throws Exception {
    givenDistributing(false);

    mockMvc.perform(post("/v1/subscription/distribute")).andExpect(status().isAccepted());

    verify(subscriber).isDistributing();
    verify(subscriber).distribute();
  }

  @Test
  void applySubscriptionsShouldRespondWithLockedIfDistributing() throws Exception {
    givenDistributing(true);

    mockMvc.perform(post("/v1/subscription/distribute")).andExpect(status().isLocked());

    verify(subscriber).isDistributing();
    verify(subscriber, never()).distribute();
  }

  @Test
  void applySubscriptionsShouldRespondWithAcceptedThenLocked() throws Exception {
    givenNotDistributingButAfterwards();

    mockMvc.perform(post("/v1/subscription/distribute")).andExpect(status().isAccepted());
    mockMvc.perform(post("/v1/subscription/distribute")).andExpect(status().isLocked());

    verify(subscriber, times(2)).isDistributing();
    verify(subscriber).distribute();
  }

  private void givenDistributing(boolean is) {
    when(subscriber.isDistributing()).thenReturn(is);
  }

  private void givenNotDistributingButAfterwards() {
    when(subscriber.isDistributing()).thenReturn(false).thenReturn(true);
  }
}
