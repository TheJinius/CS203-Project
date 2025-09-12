package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
@ActiveProfiles("test") // loads application-test.properties
public class ProductRepositoryTest {

	@Autowired
	private ProductRepository repository;

	@Test
	void testSaveAndFindById() {
		Product product = TestEntityFactory.createProduct();
		repository.save(product);
		Product found = repository.findById(product.getTlCode()).orElse(null);

		assertThat(found).isNotNull();
		assertThat(found.getTlCode()).isEqualTo(product.getTlCode());
		assertThat(found.getDescription()).isEqualTo(product.getDescription());
		assertThat(found.getTariffSchedules()).isEqualTo(product.getTariffSchedules());
	}

	@Test
	void testDelete() {
		Product product = TestEntityFactory.createProduct();
		repository.save(product);
		
		repository.deleteById(product.getTlCode());
		assertThat(repository.findById(product.getTlCode())).isEmpty();
	}
}
