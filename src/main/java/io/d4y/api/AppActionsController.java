package io.d4y.api;

import io.d4y.api.dto.HoldView;
import io.d4y.api.dto.OperationRequests;
import io.d4y.app.OperationsService;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ExecResult;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Operative Aktionen auf Applications (ADR-0013). Schreibend, aber <b>nie</b> den Sollzustand
 * verändernd. Der Akteur kommt aus {@code X-Actor} (Default {@code operator}).
 */
@RestController
@RequestMapping("/api/apps")
public class AppActionsController {

    private final OperationsService ops;

    public AppActionsController(OperationsService ops) {
        this.ops = ops;
    }

    @PostMapping("/{name}/restart")
    public Map<String, String> restart(@PathVariable String name,
                                       @RequestHeader(value = "X-Actor", required = false) String actor) {
        ops.restart(name, actor(actor));
        return Map.of("result", "OK");
    }

    @PostMapping("/{name}/stop")
    public HoldView stop(@PathVariable String name,
                         @RequestBody(required = false) OperationRequests.Stop body,
                         @RequestHeader(value = "X-Actor", required = false) String actor) {
        long dur = OperationRequests.duration(body == null ? null : body.durationSeconds());
        Hold hold = ops.stop(name, dur, actor(actor));
        return HoldView.of(hold, ops.remainingSeconds(hold));
    }

    @PostMapping("/{name}/params")
    public HoldView params(@PathVariable String name,
                           @RequestBody OperationRequests.Params body,
                           @RequestHeader(value = "X-Actor", required = false) String actor) {
        Map<String, String> env = body == null || body.env() == null ? Map.of() : body.env();
        long dur = OperationRequests.duration(body == null ? null : body.durationSeconds());
        Hold hold = ops.tempParams(name, env, dur, actor(actor));
        return HoldView.of(hold, ops.remainingSeconds(hold));
    }

    @GetMapping("/{name}")
    public ContainerDetails inspect(@PathVariable String name) {
        return ops.inspect(name);
    }

    @GetMapping("/{name}/logs")
    public Map<String, String> logs(@PathVariable String name, @RequestParam(required = false) Integer tail) {
        return Map.of("output", ops.logs(name, tail));
    }

    @PostMapping("/{name}/exec")
    public ExecResult exec(@PathVariable String name,
                           @RequestBody OperationRequests.Exec body,
                           @RequestHeader(value = "X-Actor", required = false) String actor) {
        return ops.exec(name, body.cmd(), actor(actor));
    }

    @PostMapping("/{name}/hold")
    public HoldView setHold(@PathVariable String name,
                            @RequestBody(required = false) OperationRequests.HoldReq body,
                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        HoldType type = body == null || body.type() == null ? HoldType.MANUAL : body.type();
        long dur = OperationRequests.duration(body == null ? null : body.durationSeconds());
        Hold hold = ops.setHold(name, type, dur, actor(actor));
        return HoldView.of(hold, ops.remainingSeconds(hold));
    }

    @DeleteMapping("/{name}/hold")
    public ResponseEntity<Void> releaseHold(@PathVariable String name,
                                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        ops.releaseHold(name, actor(actor));
        return ResponseEntity.noContent().build();
    }

    private static String actor(String header) {
        return header == null || header.isBlank() ? "operator" : header;
    }
}
