package com.ubs.tariffapp.repositories;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.Product;

@DataJpaTest
public class ProductRepositoryTest {
	private static final String TL_CODE = "72299005";
	private static final String DESCRIPTION = "Alloy high-speed steel, wire";
	private static final Integer DIGITS = TL_CODE.length();

	@Autowired
	private ProductRepository repository;

	@Test
	void testSaveAndFindById() {
		Product product = new Product(TL_CODE, DESCRIPTION, DIGITS, Collections.emptyList());
		repository.save(product);
		Product found = repository.findById(TL_CODE).orElse(null);
		assertThat(found).isNotNull();
		assertThat(found.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(found.getDigits()).isEqualTo(DIGITS);
	}

	@Test
	void testDelete() {
		Product product = new Product(TL_CODE, DESCRIPTION, DIGITS, Collections.emptyList());
		repository.save(product);
		repository.deleteById(TL_CODE);
		assertThat(repository.findById(TL_CODE)).isEmpty();
	}
}
