package ms_carrito.carrito.controller;

import ms_carrito.carrito.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms_carrito.carrito.dto.CarritoResponseDTO;
import ms_carrito.carrito.dto.ItemCarritoRequestDTO;
import ms_carrito.carrito.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {
    private final CarritoService service;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<CarritoResponseDTO> obtener(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obtenerCarrito(usuarioId));
    }

    @PostMapping("/{usuarioId}/items")
    public ResponseEntity<CarritoResponseDTO> agregar(@PathVariable Long usuarioId,
                                                      @Valid @RequestBody ItemCarritoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarItem(usuarioId, dto));
    }

    @DeleteMapping("/{usuarioId}/items/{itemId}")
    public ResponseEntity<CarritoResponseDTO> quitar(@PathVariable Long usuarioId,
                                                     @PathVariable Long itemId) {
        return ResponseEntity.ok(service.quitarItem(usuarioId, itemId));
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> vaciar(@PathVariable Long usuarioId) {
        service.vaciarCarrito(usuarioId);
        return ResponseEntity.noContent().build();
    }

}
