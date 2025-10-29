package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.MenuItemReview;
import edu.ucsb.cs156.example.repositories.MenuItemReviewRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = MenuItemReviewController.class)
@Import(TestConfig.class)
public class MenuItemReviewControllerTests extends ControllerTestCase {

  @MockBean MenuItemReviewRepository menuItemReviewRepository;

  @MockBean UserRepository userRepository;

  // Authorization tests for /api/ucsbdates/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/menuitemreviews/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/menuitemreviews/all")).andExpect(status().is(200)); // logged
  }

  // Authorization tests for /api/ucsbdates/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/menuitemreviews/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/menuitemreviews/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_menuitemreviews() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-04-21T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(27L)
            .reviewerEmail("cgaucho@ucsb.edu")
            .stars(3)
            .dateReviewed(ldt1)
            .comments("review1test")
            .build();

    MenuItemReview review2 =
        MenuItemReview.builder()
            .itemId(28L)
            .reviewerEmail("test2@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt2)
            .comments("review2test")
            .build();

    ArrayList<MenuItemReview> expectedReviews = new ArrayList<>();
    expectedReviews.addAll(Arrays.asList(review1, review2));

    when(menuItemReviewRepository.findAll()).thenReturn(expectedReviews);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/menuitemreviews/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(menuItemReviewRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedReviews);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_menuitemreview() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview reviewToBeSaved =
        MenuItemReview.builder()
            .itemId(10L)
            .reviewerEmail("admin@ucsb.edu")
            .stars(4)
            .dateReviewed(ldt1)
            .comments("admin post rev1")
            .build();

    MenuItemReview reviewSaved =
        MenuItemReview.builder()
            .id(1L)
            .itemId(10L)
            .reviewerEmail("admin@ucsb.edu")
            .stars(4)
            .dateReviewed(ldt1)
            .comments("admin post rev1")
            .build();

    when(menuItemReviewRepository.save(eq(reviewToBeSaved))).thenReturn(reviewSaved);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/menuitemreviews/post?itemId=10&reviewerEmail=admin@ucsb.edu&stars=4&dateReviewed=2022-01-03T00:00:00&comments=admin post rev1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).save(reviewToBeSaved);
    String expectedJson = mapper.writeValueAsString(reviewSaved);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview menuItemReview =
        MenuItemReview.builder()
            .itemId(29L)
            .reviewerEmail("test2@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt)
            .comments("review2test")
            .build();

    when(menuItemReviewRepository.findById(eq(29L))).thenReturn(Optional.of(menuItemReview));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/menuitemreviews?id=29")).andExpect(status().isOk()).andReturn();

    // assert

    verify(menuItemReviewRepository, times(1)).findById(eq(29L));
    String expectedJson = mapper.writeValueAsString(menuItemReview);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(menuItemReviewRepository.findById(eq(29L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/menuitemreviews?id=29"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(menuItemReviewRepository, times(1)).findById(eq(29L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 29 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_menuitemreview() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

    MenuItemReview menuItemReviewOrig =
        MenuItemReview.builder()
            .id(67L)
            .itemId(10L)
            .reviewerEmail("admin@ucsb.edu")
            .stars(2)
            .dateReviewed(ldt1)
            .comments("admintest")
            .build();

    MenuItemReview menuItemReviewEdited =
        MenuItemReview.builder()
            .id(67L)
            .itemId(11L)
            .reviewerEmail("admin2@ucsb.edu")
            .stars(3)
            .dateReviewed(ldt2)
            .comments("admintest update")
            .build();

    String requestBody = mapper.writeValueAsString(menuItemReviewEdited);

    when(menuItemReviewRepository.findById(eq(67L))).thenReturn(Optional.of(menuItemReviewOrig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/menuitemreviews?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(67L);
    verify(menuItemReviewRepository, times(1)).save(menuItemReviewOrig);
    assertEquals(menuItemReviewEdited.getItemId(), menuItemReviewOrig.getItemId());
    assertEquals(menuItemReviewEdited.getReviewerEmail(), menuItemReviewOrig.getReviewerEmail());
    assertEquals(menuItemReviewEdited.getStars(), menuItemReviewOrig.getStars());
    assertEquals(menuItemReviewEdited.getDateReviewed(), menuItemReviewOrig.getDateReviewed());
    assertEquals(menuItemReviewEdited.getComments(), menuItemReviewOrig.getComments());
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_menuitemreview_that_does_not_exist() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview ucsbEditedReview =
        MenuItemReview.builder()
            .itemId(2L)
            .reviewerEmail("notadmin@ucsb.edu")
            .stars(1)
            .dateReviewed(ldt1)
            .comments("not admintest")
            .build();

    String requestBody = mapper.writeValueAsString(ucsbEditedReview);

    when(menuItemReviewRepository.findById(eq(2L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/menuitemreviews?id=2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(2L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("MenuItemReview with id 2 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_a_review() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(3L)
            .reviewerEmail("notadmin@ucsb.edu")
            .stars(1)
            .dateReviewed(ldt1)
            .comments("not admintest delete")
            .build();

    when(menuItemReviewRepository.findById(eq(3L))).thenReturn(Optional.of(review1));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/menuitemreviews?id=3").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(3L);
    verify(menuItemReviewRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("MenuItemReview with id 3 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_non_existant_review_and_gets_right_error_message()
      throws Exception {
    // arrange

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/menuitemreviews?id=3").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(3L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("MenuItemReview with id 3 not found", json.get("message"));
  }
}
