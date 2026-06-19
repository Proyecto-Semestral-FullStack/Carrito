package ms_carrito.carrito.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "ms-usuarios")
public interface UsuarioClient {
    @GetMapping("/api/usuarios/id/{id}")
    void validarUsuario(@PathVariable("id") Long usuarioId);

}
