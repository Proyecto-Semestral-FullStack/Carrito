package ms_carrito.carrito.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO DE REQUEST: Agregar Item al Carrito
 *
 * Datos enviados por el cliente para agregar un producto al carrito.
 *
 * VALIDACIONES:
 * - productoId: Requerido, debe existir en ms-catalogo
 * - cantidad: Mínimo 1, défault 1
 *
 * USADO EN:
 * - POST /api/carrito/{usuarioId}/items
 */
@Data
@Schema(
        name = "ItemCarritoRequestDTO",
        description = """
                Datos para agregar un producto al carrito.
                
                EJEMPLO DE REQUEST:
                {
                  "productoId": 5,
                  "cantidad": 2
                }
                
                VALIDACIONES APLICADAS:
                1. productoId: @NotNull (requerido)
                2. cantidad: @Min(1) (mínimo 1 unidad)
                
                VALIDACIONES DE NEGOCIO (en ms-carrito):
                1. Usuario existe
                2. Producto existe en ms-catalogo
                3. Cantidad disponible en ms-inventario
                """,
        example = """
                {
                  "productoId": 5,
                  "cantidad": 2
                }
                """
)
public class ItemCarritoRequestDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    @Schema(
            description = """
                    ID único del producto en ms-catalogo que se desea agregar.
                    
                    VALIDACIONES:
                    - Requerido (no puede ser null)
                    - Debe ser un número positivo (≥ 1)
                    - El producto debe existir en ms-catalogo
                    - El producto debe tener stock disponible
                    
                    EJEMPLO:
                    "productoId": 5
                    """,
            type = "integer",
            format = "int64",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long productoId;

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Schema(
            description = """
                    Cantidad de unidades a agregar al carrito.
                    
                    RESTRICCIONES:
                    - Mínimo: 1
                    - Máximo: depende de stock en ms-inventario
                    - Válido: cualquier número entero ≥ 1
                    
                    VALIDACIONES:
                    - @Min(1): Garantiza que sea ≥ 1
                    - Stock check: En el backend se verifica con ms-inventario
                    
                    DÉFAULT:
                    Si no se proporciona, se asume 1
                    
                    EJEMPLO VÁLIDOS:
                    - 1 unidad: "cantidad": 1
                    - 10 unidades: "cantidad": 10
                    - 100 unidades: "cantidad": 100
                    
                    EJEMPLOS INVÁLIDOS:
                    - 0 unidades: ERROR (mínimo 1)
                    - -5 unidades: ERROR (negativo)
                    - ABC: ERROR (no es número)
                    """,
            type = "integer",
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Integer cantidad = 1;
}