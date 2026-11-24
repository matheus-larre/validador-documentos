package br.com.matheuslarre.validadordocumentos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fornece o "relógio" da aplicação como um bean.
 * Quem precisa da data de hoje recebe o Clock por injeção em vez de chamar LocalDate.now() direto:
 * assim os testes conseguem fixar o "hoje" numa data conhecida.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock() {
        // Fuso fixo do Brasil: dentro do Docker o padrão é UTC, o que mudaria a data depois das 21h
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
