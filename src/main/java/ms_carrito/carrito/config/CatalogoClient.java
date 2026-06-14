package ms_carrito.carrito.config;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {
    @GetMapping("/api/productos/{id}")
    CatalogoProductoInfo obtenerProducto(@PathVariable("id") Long productoId);

    @Data
    class CatalogoProductoInfo {
        private String nombre;
        private BigDecimal precio;
        // getters y setters (puedes usar @Data de Lombok)
    }

}
