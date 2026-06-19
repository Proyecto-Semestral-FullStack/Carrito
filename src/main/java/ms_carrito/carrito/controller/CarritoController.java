package ms_carrito.carrito.controller;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms_carrito.carrito.dto.CarritoResponseDTO;
import ms_carrito.carrito.dto.ItemCarritoRequestDTO;
import ms_carrito.carrito.exception.ErrorResponse;
import ms_carrito.carrito.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLADOR DE CARRITO DE COMPRAS
 *
 * Proporciona operaciones REST para gestionar el carrito de compras del usuario.
 * Permite obtener, agregar items, quitar items y vaciar el carrito.
 *
 * MICROSERVICIO: ms-carrito | VERSIÓN: 1.0.0
 */
@RestController
@RequestMapping(
        value = "/api/carrito",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Tag(
        name = "Carrito de Compras",
        description = """
                Gestión completa del carrito de compras del usuario.
                
                RESPONSABILIDADES:
                - Obtener estado actual del carrito activo
                - Agregar productos al carrito (con validaciones de usuario, stock e inventario)
                - Quitar items individuales del carrito
                - Vaciar completamente el carrito
                
                REGLAS DE NEGOCIO:
                1. Un usuario solo puede tener UN carrito en estado ACTIVO
                2. Antes de agregar items se valida: usuario existe → producto existe → stock disponible
                3. Cada cambio en el carrito actualiza el timestamp 'actualizadoEn'
                4. Los prices y nombres se capturan como snapshots (inmutables en el carrito)
                
                FLUJO TÍPICO DE NEGOCIO:
                1. Cliente consulta GET /api/carrito/{usuarioId}
                2. Cliente agrega items POST /api/carrito/{usuarioId}/items
                3. Cliente puede quitar items DELETE /api/carrito/{usuarioId}/items/{itemId}
                4. Al checkout, se valida el carrito en este microservicio
                
                INTEGRACIÓN CON OTROS MICROSERVICIOS:
                - ms-usuarios: Validar que el usuario existe
                - ms-catalogo: Obtener nombre y precio del producto
                - ms-inventario: Verificar stock disponible
                """,
        externalDocs = @ExternalDocumentation(
                description = "Ver documentación completa en Confluence"
        )
)
public class CarritoController {

    private final CarritoService service;

    @GetMapping("/{usuarioId}")
    @Operation(
            summary = "Obtener carrito activo del usuario",
            description = """
                    Recupera el carrito de compras activo del usuario especificado.
                    
                    FLUJO:
                    1. Validar que el usuarioId sea válido (≥ 1)
                    2. Buscar carrito con estado ACTIVO para este usuario
                    3. Si existe, retornar con todos los items y cálculos
                    4. Si no existe, lanzar 404 NOT_FOUND
                    """,
            operationId = "obtenerCarrito",
            tags = {"Consultas"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Carrito obtenido exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CarritoResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "carrito-con-items",
                                    summary = "Carrito con 2 items",
                                    value = """
                                            {
                                              "id": 42,
                                              "usuarioId": 1,
                                              "estado": "ACTIVO",
                                              "items": [
                                                {
                                                  "id": 100,
                                                  "productoId": 5,
                                                  "nombreProducto": "Laptop Gaming ROG",
                                                  "cantidad": 1,
                                                  "precioUnitario": 1599.99,
                                                  "subtotal": 1599.99
                                                },
                                                {
                                                  "id": 101,
                                                  "productoId": 12,
                                                  "nombreProducto": "Mouse Logitech",
                                                  "cantidad": 2,
                                                  "precioUnitario": 49.99,
                                                  "subtotal": 99.98
                                                }
                                              ],
                                              "total": 1699.97,
                                              "actualizadoEn": "2026-06-18T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Carrito no encontrado para el usuario",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "carrito-no-encontrado",
                                    summary = "Usuario sin carrito activo",
                                    value = """
                                            {
                                              "status": 404,
                                              "mensaje": "Carrito no encontrado para el usuario 999",
                                              "timestamp": "2026-06-18T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<CarritoResponseDTO> obtener(
            @PathVariable
            @Parameter(
                    name = "usuarioId",
                    description = "ID único del usuario (referencia a ms-usuarios)",
                    example = "1",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            Long usuarioId
    ) {
        return ResponseEntity.ok(service.obtenerCarrito(usuarioId));
    }

    @PostMapping("/{usuarioId}/items")
    @Operation(
            summary = "Agregar producto al carrito",
            description = """
                    Agrega un nuevo item al carrito del usuario.
                    
                    VALIDACIONES EJECUTADAS (en orden):
                    1. Usuario existe en ms-usuarios
                    2. Producto existe en ms-catalogo (obtiene nombre y precio)
                    3. Cantidad solicitada está disponible en ms-inventario
                    4. Si no existe carrito ACTIVO, se crea uno
                    5. Se agrega el item con datos snapshot (inmutables)
                    6. Se retorna el carrito actualizado
                    
                    IMPORTANTE:
                    - Si falla cualquier validación, NO se agrega el item
                    - El nombre y precio se capturan como snapshot
                    - La cantidad se valida contra stock actual
                    """,
            operationId = "agregarItem",
            tags = {"Mutaciones"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Item agregado exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CarritoResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "carrito-item-agregado",
                                    summary = "Carrito después de agregar item",
                                    value = """
                                            {
                                              "id": 42,
                                              "usuarioId": 1,
                                              "estado": "ACTIVO",
                                              "items": [
                                                {
                                                  "id": 102,
                                                  "productoId": 5,
                                                  "nombreProducto": "Laptop Gaming ROG",
                                                  "cantidad": 2,
                                                  "precioUnitario": 1599.99,
                                                  "subtotal": 3199.98
                                                }
                                              ],
                                              "total": 3199.98,
                                              "actualizadoEn": "2026-06-18T10:35:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos (productoId, cantidad)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "validacion-fallida",
                                    summary = "Cantidad menor a 1",
                                    value = """
                                            {
                                              "status": 400,
                                              "mensaje": "La cantidad debe ser al menos 1",
                                              "timestamp": "2026-06-18T10:35:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso no encontrado (usuario o producto no existe)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "usuario-no-existe",
                                            summary = "Usuario no existe en ms-usuarios",
                                            value = """
                                                    {
                                                      "status": 404,
                                                      "mensaje": "Usuario no encontrado: 999",
                                                      "timestamp": "2026-06-18T10:35:00"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "producto-no-existe",
                                            summary = "Producto no existe en ms-catalogo",
                                            value = """
                                                    {
                                                      "status": 404,
                                                      "mensaje": "Producto no encontrado en catálogo: 999",
                                                      "timestamp": "2026-06-18T10:35:00"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Stock insuficiente (cantidad solicitada > disponible)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "stock-insuficiente",
                                    summary = "No hay suficiente stock del producto",
                                    value = """
                                            {
                                              "status": 409,
                                              "mensaje": "Stock insuficiente para el producto 5",
                                              "timestamp": "2026-06-18T10:35:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<CarritoResponseDTO> agregar(
            @PathVariable
            @Parameter(
                    name = "usuarioId",
                    description = "ID único del usuario (referencia a ms-usuarios)",
                    example = "1",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            Long usuarioId,
            @Valid
            @RequestBody(
                    description = "Datos del item a agregar al carrito",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ItemCarritoRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "agregar-1-item",
                                            summary = "Agregar 1 unidad de un producto",
                                            value = """
                                                    {
                                                      "productoId": 5,
                                                      "cantidad": 1
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "agregar-multiples",
                                            summary = "Agregar múltiples unidades",
                                            value = """
                                                    {
                                                      "productoId": 12,
                                                      "cantidad": 3
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            ItemCarritoRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarItem(usuarioId, dto));
    }

    @DeleteMapping("/{usuarioId}/items/{itemId}")
    @Operation(
            summary = "Quitar item del carrito",
            description = """
                    Elimina un item específico del carrito del usuario.
                    
                    COMPORTAMIENTO:
                    1. Buscar carrito ACTIVO del usuario
                    2. Eliminar el item si existe
                    3. Si el item NO existe, es operación SEGURA (no falla)
                    4. Actualizar timestamp
                    5. Retorna carrito actualizado
                    
                    NOTA: Operación idempotente.
                    """,
            operationId = "quitarItem",
            tags = {"Mutaciones"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item removido exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CarritoResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "carrito-sin-item",
                                    summary = "Carrito después de quitar item",
                                    value = """
                                            {
                                              "id": 42,
                                              "usuarioId": 1,
                                              "estado": "ACTIVO",
                                              "items": [],
                                              "total": 0.00,
                                              "actualizadoEn": "2026-06-18T10:40:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Carrito no encontrado para el usuario",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<CarritoResponseDTO> quitar(
            @PathVariable
            @Parameter(
                    name = "usuarioId",
                    description = "ID único del usuario",
                    example = "1",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            Long usuarioId,
            @PathVariable
            @Parameter(
                    name = "itemId",
                    description = "ID único del item a eliminar",
                    example = "100",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            Long itemId
    ) {
        return ResponseEntity.ok(service.quitarItem(usuarioId, itemId));
    }

    @DeleteMapping("/{usuarioId}")
    @Operation(
            summary = "Vaciar carrito completamente",
            description = """
                    Elimina TODOS los items del carrito, dejándolo vacío pero activo.
                    
                    FLUJO:
                    1. Buscar carrito ACTIVO del usuario
                    2. Eliminar todos los items
                    3. Carrito queda ACTIVO y vacío (listo para nuevos items)
                    4. Actualizar timestamp
                    5. Retorna 204 NO CONTENT (sin body)
                    
                    NOTA IMPORTANTE:
                    - El carrito NO se ELIMINA, solo se VACÍA
                    - El estado permanece ACTIVO
                    - Se puede seguir utilizando el mismo carrito
                    """,
            operationId = "vaciarCarrito",
            tags = {"Mutaciones"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Carrito vaciado exitosamente (sin contenido en respuesta)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Carrito no encontrado para el usuario",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<Void> vaciar(
            @PathVariable
            @Parameter(
                    name = "usuarioId",
                    description = "ID único del usuario cuyo carrito se va a vaciar",
                    example = "1",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            Long usuarioId
    ) {
        service.vaciarCarrito(usuarioId);
        return ResponseEntity.noContent().build();
    }
}

/*
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

 */
