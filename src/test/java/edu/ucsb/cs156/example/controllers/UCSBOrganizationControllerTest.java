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
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTest extends ControllerTestCase {

  @MockBean UCSBOrganizationRepository ucsbOrganizationRepository;

  @MockBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganization/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/ucsborganization/all")).andExpect(status().is(200)); // logged
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganization?orgCode=MTG"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/ucsborganization/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    UCSBOrganization mtg =
        UCSBOrganization.builder()
            .orgCode("MTG")
            .orgTranslationShort("MTG@UCSB")
            .orgTranslation("Magic: the Gathering at UCSB")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("MTG"))).thenReturn(Optional.of(mtg));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=MTG"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("MTG"));
    String expectedJson = mapper.writeValueAsString(mtg);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(ucsbOrganizationRepository.findById(eq("GG"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=GG"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("GG"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id GG not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/ucsborganization/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsborganizations() throws Exception {

    // arrange

    UCSBOrganization mtg =
        UCSBOrganization.builder()
            .orgCode("MTG")
            .orgTranslationShort("MTG@UCSB")
            .orgTranslation("Magic: the Gathering at UCSB")
            .inactive(false)
            .build();

    UCSBOrganization bgc =
        UCSBOrganization.builder()
            .orgCode("TGC")
            .orgTranslationShort("Tabletop Gaming Club")
            .orgTranslation("UCSB Tabletop Gaming Club")
            .inactive(false)
            .build();

    ArrayList<UCSBOrganization> expectedOrganizations = new ArrayList<>();
    expectedOrganizations.addAll(Arrays.asList(mtg, bgc));

    when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrganizations);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/ucsborganization/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedOrganizations);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_organization() throws Exception {
    // arrange

    UCSBOrganization gg =
        UCSBOrganization.builder()
            .orgCode("GG")
            .orgTranslationShort("UCSB-GG")
            .orgTranslation("UCSB-Gaucho-Gaming")
            .inactive(true)
            .build();

    when(ucsbOrganizationRepository.save(eq(gg))).thenReturn(gg);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/ucsborganization/post?orgCode=GG&orgTranslationShort=UCSB-GG&orgTranslation=UCSB-Gaucho-Gaming&inactive=true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).save(gg);
    String expectedJson = mapper.writeValueAsString(gg);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
