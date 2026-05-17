package ms_carrito.carrito.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class InventarioClient {
    private final WebClient webClient;

    public InventarioClient(@Value("${inventario.service.url}") String inventarioUrl) {
        this.webClient = WebClient.builder().baseUrl(inventarioUrl).build();
    }

    /**
     * Verifica si hay stock suficiente para un producto.
     * Retorna true si la cantidad disponible es >= cantidadSolicitada.
     */
    public boolean verificarStock(Long productoId, int cantidadSolicitada) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/stock/{productoId}", productoId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("cantidadDisponible") == null) {
                return false;
            }

            int cantidadDisponible = ((Number) response.get("cantidadDisponible")).intValue();
            return cantidadDisponible >= cantidadSolicitada;

        } catch (Exception e) {
            log.error("Error al verificar stock con ms-inventario: {}", e.getMessage());
            return false; // Si falla la conexión, no permitimos agregar (modo seguro)
        }
    }
}
