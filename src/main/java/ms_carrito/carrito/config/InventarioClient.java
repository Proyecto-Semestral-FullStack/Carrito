package ms_carrito.carrito.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
@FeignClient(name = "ms-inventario")
public interface InventarioClient {
    @GetMapping("/api/stock/disponible")
    Map<String, Object> verificarStock(@RequestParam("productoId") Long productoId,
                                       @RequestParam("cantidad") int cantidad);
}
