package ms_carrito.carrito.webclient;
import ms_carrito.carrito.exception.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;


@Component
@Slf4j
public class UsuarioClient {

    private final WebClient webClient;

    public UsuarioClient(@Value("${usuario.service.url}") String usuarioUrl) {
        this.webClient = WebClient.builder().baseUrl(usuarioUrl).build();
    }

    public void validarUsuario(Long usuarioId) {
        try {
            webClient.get()
                    .uri("/api/usuarios/{id}", usuarioId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> { throw new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId); })
                    .bodyToMono(Map.class)
                    .block();
        } catch (RecursoNoEncontradoException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al conectar con ms-usuarios: {}", e.getMessage());
            throw new RuntimeException("No se pudo validar el usuario", e);
        }
    }
}
