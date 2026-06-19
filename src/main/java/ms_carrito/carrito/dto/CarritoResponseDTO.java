package ms_carrito.carrito.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO DE RESPUESTA: Carrito de Compras
 *
 * Representa el estado actual del carrito de un usuario.
 * Incluye todos los items con cálculos de subtotales y total.
 *
 * IMPORTANTE:
 * - Todos los campos son de SOLO LECTURA (serializados en respuestas)
 * - Los datos de producto (nombre, precio) son SNAPSHOTS inmutables
 * - El total se calcula automáticamente en el servidor
 *
 * USADO EN:
 * - GET /api/carrito/{usuarioId}
 * - POST /api/carrito/{usuarioId}/items (201 CREATED)
 * - DELETE /api/carrito/{usuarioId}/items/{itemId}
 *
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CarritoResponseDTO",
        description = """
                Representa el carrito de compras del usuario con todos sus items.
                
                ESTRUCTURA:
                - id: Identificador único del carrito en la BD
                - usuarioId: Referencia al usuario propietario
                - estado: Estado actual (ACTIVO, PROCESADO, ABANDONADO)
                - items: Lista de items en el carrito
                - total: Suma de todos los subtotales (BigDecimal con 2 decimales)
                - actualizadoEn: Timestamp UTC de la última modificación
                
                GARANTÍAS:
                1. El carrito retornado está en estado ACTIVO
                2. El total es la suma correcta de subtotales
                3. Cada item tiene precio y nombre inmutables
                4. El timestamp está en formato ISO 8601
                """,
        example = """
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
public class CarritoResponseDTO {

    @Schema(
            description = "Identificador único del carrito en la base de datos",
            type = "integer",
            format = "int64",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "ID del usuario propietario del carrito (referencia a ms-usuarios)",
            type = "integer",
            format = "int64",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long usuarioId;

    @Schema(
            description = """
                    Estado actual del carrito.
                    
                    VALORES POSIBLES:
                    - ACTIVO: Carrito en uso, puede agregarse/quitarse items
                    - PROCESADO: Carrito completado y pagado (no modificable)
                    - ABANDONADO: Carrito abandonado por el usuario (no modificable)
                    """,
            type = "string",
            example = "ACTIVO",
            allowableValues = {"ACTIVO", "PROCESADO", "ABANDONADO"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String estado;

    @Schema(
            description = "Lista de items en el carrito. Array vacío si no hay items.",
            type = "array",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<ItemResponseDTO> items;

    @Schema(
            description = """
                    Total del carrito (suma de todos los subtotales).
                    
                    CÁLCULO:
                    total = Σ(precioUnitario × cantidad) para cada item
                    
                    PRECISIÓN:
                    - BigDecimal con 2 decimales
                    - Moneda: USD (pendiente de confirmación)
                    
                    EJEMPLO:
                    - Item 1: 1599.99 × 1 = 1599.99
                    - Item 2: 49.99 × 2 = 99.98
                    - Total: 1699.97
                    """,
            type = "string",
            format = "decimal",
            pattern = "^\\d+(\\.\\d{1,2})?$",
            example = "1699.97",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal total;

    @Schema(
            description = """
                    Fecha y hora UTC de la última modificación del carrito.
                    
                    FORMATO:
                    - ISO 8601: YYYY-MM-DDTHH:mm:ss
                    - Zona horaria: UTC
                    
                    SE ACTUALIZA CUANDO:
                    - Se agrega un item
                    - Se quita un item
                    - Se vacía el carrito
                    - Se realiza cualquier cambio
                    """,
            type = "string",
            format = "date-time",
            example = "2026-06-18T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualizadoEn;

    /**
     * DTO DE RESPUESTA: Item del Carrito
     *
     * Representa un producto agregado al carrito.
     * Los datos de producto (nombre, precio) son SNAPSHOTS capturados en el momento de agregar.
     *
     * IMPORTANTE:
     * - El precio es INMUTABLE (captura del precio al agregar)
     * - El nombre es INMUTABLE (captura del nombre al agregar)
     * - Si el precio del producto cambia en catálogo, NO afecta los items existentes
     *
     * @author Equipo Backend
     * @version 1.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "ItemResponseDTO",
            description = """
                    Representa un item del carrito con snapshots de producto.
                    
                    SNAPSHOTS:
                    Los valores nombreProducto y precioUnitario son SNAPSHOTS (copias inmutables)
                    del estado del producto cuando fue agregado. Si el precio o nombre cambian
                    en ms-catalogo, estos valoresaquí NO se ven afectados.
                    
                    VENTAJA:
                    El cliente ve exactamente qué precio tenía cuando decidió agregar el producto.
                    No hay sorpresas de cambios de precio posteriores.
                    """,
            example = """
                    {
                      "id": 100,
                      "productoId": 5,
                      "nombreProducto": "Laptop Gaming ROG",
                      "cantidad": 1,
                      "precioUnitario": 1599.99,
                      "subtotal": 1599.99
                    }
                    """
    )
    public static class ItemResponseDTO {

        @Schema(
                description = "Identificador único del item en el carrito",
                type = "integer",
                format = "int64",
                example = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long id;

        @Schema(
                description = "ID del producto en ms-catalogo (referencia externa)",
                type = "integer",
                format = "int64",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long productoId;

        @Schema(
                description = """
                        Nombre del producto (SNAPSHOT - INMUTABLE).
                        
                        Este es el nombre que tenía el producto cuando fue agregado al carrito.
                        Si el nombre cambia en ms-catalogo, aquí permanece igual.
                        
                        RAZÓN:
                        Proporciona trazabilidad: el cliente sabe exactamente qué compró.
                        """,
                type = "string",
                example = "Laptop Gaming ROG",
                minLength = 1,
                maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String nombreProducto;

        @Schema(
                description = """
                        Cantidad de unidades del producto en el carrito.
                        
                        RANGO:
                        - Mínimo: 1
                        - Máximo: depende de stock disponible
                        - Típico: 1-10
                        
                        NOTA:
                        Si se modifica, se debe llamar a quitar() y agregar() de nuevo
                        (no hay endpoint PATCH para modificar cantidad).
                        """,
                type = "integer",
                example = "2",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer cantidad;

        @Schema(
                description = """
                        Precio unitario del producto (SNAPSHOT - INMUTABLE).
                        
                        Este es el precio que tenía cuando fue agregado.
                        Si el precio cambia en ms-catalogo, aquí permanece igual.
                        
                        PRECISIÓN:
                        - BigDecimal con 2 decimales
                        - Rango: 0.01 a 999999.99
                        """,
                type = "string",
                format = "decimal",
                pattern = "^\\d+(\\.\\d{1,2})?$",
                example = "1599.99",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private BigDecimal precioUnitario;

        @Schema(
                description = """
                        Subtotal del item (cantidad × precioUnitario).
                        
                        CÁLCULO:
                        subtotal = cantidad × precioUnitario
                        
                        EJEMPLO:
                        cantidad: 2
                        precioUnitario: 49.99
                        subtotal: 99.98
                        
                        PRECISIÓN:
                        - BigDecimal con 2 decimales (redondeado)
                        """,
                type = "string",
                format = "decimal",
                pattern = "^\\d+(\\.\\d{1,2})?$",
                example = "3199.98",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private BigDecimal subtotal;
    }
}

