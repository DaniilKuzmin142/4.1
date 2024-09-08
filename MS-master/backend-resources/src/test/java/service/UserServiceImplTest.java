package service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.itm.space.backendresources.BackendResourcesApplication;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = BackendResourcesApplication.class)
class UserServiceImplTest {

    private WireMockServer wireMockServer;

    @Mock
    private Keycloak keycloakClient;

    @InjectMocks
    private UserServiceImpl userService;

    @Value("${keycloak.realm}")
    private String realm;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();
        configureFor("localhost", 8080);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void createUser_shouldCreateUser() {
        UserRequest userRequest = new UserRequest("username", "email@test.com", "password", "FirstName", "LastName");

        stubFor(post(urlEqualTo("/auth/admin/realms/" + realm + "/users"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())));

        assertDoesNotThrow(() -> userService.createUser(userRequest));
    }

    @Test
    void getUserById_shouldReturnUser() {
        UUID userId = UUID.randomUUID();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(userId.toString());
        userRepresentation.setUsername("testuser");


        stubFor(get(urlEqualTo("/auth/admin/realms/" + realm + "/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("{ \"id\": \"" + userId + "\", \"username\": \"testuser\" }")));


        when(keycloakClient.realm(realm).users().get(userId.toString()).toRepresentation()).thenReturn(userRepresentation);


        List<String> roles = List.of("ROLE_USER");
        List<String> groups = List.of("GROUP_USER");


        UserResponse userResponse = new UserResponse(userId.toString(), "FirstName", "LastName", "email@test.com", roles, groups);
        when(userService.getUserById(userId)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId.toString(), result.getId());
        assertEquals("FirstName", result.getFirstName());
        assertEquals("LastName", result.getLastName());
        assertEquals("email@test.com", result.getEmail());
        assertEquals(roles, result.getRoles());
        assertEquals(groups, result.getGroups());
    }
}