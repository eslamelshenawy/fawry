package com.fawry.routing.controller;

import com.fawry.routing.dto.request.GatewayRequest;
import com.fawry.routing.dto.response.GatewayResponse;
import com.fawry.routing.service.GatewayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Gateways", description = "Manage payment gateway configurations")
@RestController
@RequestMapping("/api/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @GetMapping
    public List<GatewayResponse> list() {
        return gatewayService.listAll();
    }

    @GetMapping("/{id}")
    public GatewayResponse get(@PathVariable Long id) {
        return gatewayService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GatewayResponse> create(@Valid @RequestBody GatewayRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        GatewayResponse created = gatewayService.create(request);
        URI location = uriBuilder.path("/api/gateways/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GatewayResponse update(@PathVariable Long id, @Valid @RequestBody GatewayRequest request) {
        return gatewayService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gatewayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
