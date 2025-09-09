package com.ubs.tariffapp.repositories;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestConstants;

@DataJpaTest
public class ProductRepositoryTest {
	private static final String TL_CODE = TestConstants.PRODUCT_TL_CODE;
	private static final String DESCRIPTION = TestConstants.PRODUCT_DESCRIPTION;
	private static final Integer DIGITS = TL_CODE.length();
    private static final List<TariffSchedule> TARIFF_SCHEDULES = TestConstants.PRODUCT_TARIFF_SCHEDULES;

	@Autowired
	private ProductRepository repository;

	@Test
	void testSaveAndFindById() {
		Product product = new Product(TL_CODE, DESCRIPTION, DIGITS, TARIFF_SCHEDULES);
		repository.save(product);
		Product found = repository.findById(TL_CODE).orElse(null);
		assertThat(found).isNotNull();
		assertThat(found.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(found.getDigits()).isEqualTo(DIGITS);
	}

	@Test
	void testDelete() {
		Product product = new Product(TL_CODE, DESCRIPTION, DIGITS, TARIFF_SCHEDULES);
		repository.save(product);
		repository.deleteById(TL_CODE);
		assertThat(repository.findById(TL_CODE)).isEmpty();
	}
}
