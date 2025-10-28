package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockBean RecommendationRequestRepository recommendationrequestRepository;

  @MockBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequest/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequest/all")).andExpect(status().is(200)); // logged
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/recommendationrequest/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/recommendationrequest/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsbdates() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2025-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2025-01-03T00:00:00");

    RecommendationRequest recommendationrequest1 =
        RecommendationRequest.builder()
            .requesteremail("requesterqmail@mail.com")
            .professoremail("professorfmail@mail.com")
            .explanation("program")
            .daterequested(ldt1)
            .dateneeded(ldt2)
            .done(true)
            .build();

    ArrayList<RecommendationRequest> expectedrecRequests = new ArrayList<>();
    // expectedrecRequests.addAll(Arrays.asList(ucsbDate1, ucsbDate2));
    expectedrecRequests.add(recommendationrequest1);
    when(recommendationrequestRepository.findAll()).thenReturn(expectedrecRequests);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequest/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationrequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedrecRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendationrequest() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2025-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2025-01-03T00:00:00");

    RecommendationRequest recommendationrequest1 =
        RecommendationRequest.builder()
            .requesteremail("requesteremail@mail.com")
            .professoremail("professoremail@mail.com")
            .explanation("program")
            .daterequested(ldt1)
            .dateneeded(ldt2)
            .done(true)
            .build();

    when(recommendationrequestRepository.save(eq(recommendationrequest1)))
        .thenReturn(recommendationrequest1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequest/post?requesteremail=requesteremail@mail.com&professoremail=professoremail@mail.com&explanation=program&daterequested=2025-01-03T00:00:00&dateneeded=2025-01-03T00:00:00&done=true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationrequestRepository, times(1)).save(eq(recommendationrequest1));
    String expectedJson = mapper.writeValueAsString(recommendationrequest1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
