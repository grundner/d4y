package io.d4y.api;

import io.d4y.app.AppNotFoundException;
import io.d4y.app.OperationsService;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppActionsController.class)
class AppActionsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OperationsService ops;

    @Test
    void restartReturnsOk() throws Exception {
        mvc.perform(post("/api/apps/nginx/restart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"));
        verify(ops).restart(eq("nginx"), eq("operator"));
    }

    @Test
    void stopReturnsHoldView() throws Exception {
        Hold hold = new Hold("nginx", HoldType.STOP, Instant.parse("2026-07-07T12:05:00Z"));
        when(ops.stop(eq("nginx"), anyLong(), anyString())).thenReturn(hold);
        when(ops.remainingSeconds(any())).thenReturn(300L);

        mvc.perform(post("/api/apps/nginx/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationSeconds\":300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("nginx"))
                .andExpect(jsonPath("$.type").value("STOP"))
                .andExpect(jsonPath("$.remainingSeconds").value(300));
    }

    @Test
    void restartUnknownAppIs404() throws Exception {
        doThrow(new AppNotFoundException("nope")).when(ops).restart(anyString(), anyString());

        mvc.perform(post("/api/apps/ghost/restart")).andExpect(status().isNotFound());
    }
}
