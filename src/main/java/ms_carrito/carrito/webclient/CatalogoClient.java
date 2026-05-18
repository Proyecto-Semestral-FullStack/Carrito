package ms_carrito.carrito.webclient;
 import ms_carrito.carrito.exception.RecursoNoEncontradoException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
 import org.springframework.cloud.client.loadbalancer.LoadBalanced;
 import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.math.BigDecimal;

@Component
@Slf4j
public class CatalogoClient {
    private final WebClient webClient;

    public CatalogoClient(@LoadBalanced WebClient.Builder webClientBuilder,
                          @Value("${catalogo.service.url}") String catalogoUrl) {
        this.webClient = WebClient.builder().baseUrl(catalogoUrl).build();
    }

    public CatalogoProductoInfo obtenerProducto(Long productoId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> { throw new RecursoNoEncontradoException("Producto no encontrado en catálogo: " + productoId); })
                    .bodyToMono(Map.class)
                    .block();

            String nombre = (String) response.get("nombre");
            BigDecimal precio = new BigDecimal(response.get("precio").toString());

            return new CatalogoProductoInfo(nombre, precio);
        } catch (RecursoNoEncontradoException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al conectar con ms-catalogo: {}", e.getMessage());
            throw new RuntimeException("No se pudo obtener el producto del catálogo", e);
        }
    }

    public record CatalogoProductoInfo(String nombre, BigDecimal precio) {}
}
