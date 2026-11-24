package br.com.matheuslarre.validadordocumentos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // usa o H2 em memória: o teste não depende do PostgreSQL estar rodando
class ValidadorDocumentosApplicationTests {

	@Test
	void contextLoads() {
	}

}
