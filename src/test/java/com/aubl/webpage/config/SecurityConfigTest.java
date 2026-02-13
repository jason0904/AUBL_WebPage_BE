package com.aubl.webpage.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aubl.webpage.api.GameController;
import com.aubl.webpage.api.SeasonController;
import com.aubl.webpage.api.dto.GameCreateRequest;
import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.service.GameService;
import com.aubl.webpage.service.SeasonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {SeasonController.class, GameController.class})
@Import({SecurityConfig.class, PasswordConfig.class})
@AutoConfigureMockMvc(addFilters = true)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeasonService seasonService;

    @MockBean
    private GameService gameService;

    @Test
    void seasonCreation_requires_admin_token() throws Exception {
        Season season = new Season();
        season.setId(1L);
        when(seasonService.createSeason(any(SeasonCreateRequest.class))).thenReturn(season);

        try (MockedStatic<FirebaseAuth> firebaseAuth = mockFirebaseAuth(true)) {
            mockMvc.perform(post("/api/seasons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SeasonCreateRequest(2026)))
                    .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        }
    }

    @Test
    void seasonCreation_forbidden_when_not_admin() throws Exception {
        Season season = new Season();
        season.setId(1L);
        when(seasonService.createSeason(any(SeasonCreateRequest.class))).thenReturn(season);

        try (MockedStatic<FirebaseAuth> firebaseAuth = mockFirebaseAuth(false)) {
            mockMvc.perform(post("/api/seasons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SeasonCreateRequest(2026)))
                    .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void gameCreation_forbidden_when_no_token() throws Exception {
        Game game = new Game();
        game.setId(1L);
        when(gameService.createGame(any(GameCreateRequest.class))).thenReturn(game);

        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    private MockedStatic<FirebaseAuth> mockFirebaseAuth(boolean admin) throws Exception {
        MockedStatic<FirebaseAuth> firebaseAuth = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        firebaseAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
        when(auth.verifyIdToken(anyString())).thenReturn(token);
        when(token.getUid()).thenReturn("uid-123");
        when(token.getClaims()).thenReturn(admin ? Map.of("admin", true) : Map.of());
        return firebaseAuth;
    }
}
