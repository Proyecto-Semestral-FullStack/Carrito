package ms_carrito.carrito.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO DE RESPUESTA: Error Estándar
 *
 * Se retorna en caso de errores del cliente o del servidor.
 *
 * USADO EN:
 * - 400 Bad Request
 * - 404 Not Found
 * - 409 Conflict
 * - 500 Internal Server Error
 */
@Data
@AllArgsConstructor
@Schema(
        name = "ErrorResponse",
        description = """
                Respuesta estándar de error en todas las operaciones fallidas.
                
                ESTRUCTURA:
                - status: Código HTTP error (400, 404, 409, 500, etc.)
                - mensaje: Descripción clara del error en lenguaje natural
                - timestamp: Cuándo ocurrió el error (UTC, ISO 8601)
                
                PROPÓSITO:
                - Informar al cliente QUÉ salió mal
                - Informar CUÁNDO falló
                - Facilitar debugging y troubleshooting
                """,
        example = """
                {
                  "status": 404,
                  "mensaje": "Carrito no encontrado para el usuario 999",
                  "timestamp": "2026-06-18T10:30:00"
                }
                """
)
public class ErrorResponse {

    @Schema(
            description = """
                    Código HTTP del error.
                    
                    CÓDIGOS COMUNES:
                    - 400: Bad Request (datos inválidos)
                    - 404: Not Found (recurso no existe)
                    - 409: Conflict (violación de regla de negocio)
                    - 500: Internal Server Error (error del servidor)
                    
                    NOTA:
                    Este campo duplica el código HTTP de la respuesta,
                    pero se incluye por comodidad del cliente.
                    """,
            type = "integer",
            example = "404",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int status;

    @Schema(
            description = """
                    Mensaje de error descriptivo en lenguaje natural.
                    
                    CARACTERÍSTICA:
                    - Mensaje CLARO y ÚTIL para entender qué salió mal
                    - Incluye detalles específicos del error
                    - Apropiado para mostrar al usuario final
                    
                    EJEMPLOS:
                    - "Carrito no encontrado para el usuario 999"
                    - "Usuario no encontrado: 999"
                    - "La cantidad debe ser al menos 1"
                    - "Producto no encontrado en catálogo: 5"
                    - "Stock insuficiente para el producto 5. Disponible: 3, Solicitado: 10"
                    """,
            type = "string",
            example = "Carrito no encontrado para el usuario 999",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String mensaje;

    @Schema(
            description = """
                    Fecha y hora UTC en que ocurrió el error.
                    
                    FORMATO:
                    - ISO 8601: YYYY-MM-DDTHH:mm:ss
                    - Zona horaria: UTC
                    
                    UTILIDAD:
                    - Correlacionar con logs del servidor
                    - Entender secuencia de eventos
                    - Debugging de problemas temporales
                    """,
            type = "string",
            format = "date-time",
            example = "2026-06-18T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}