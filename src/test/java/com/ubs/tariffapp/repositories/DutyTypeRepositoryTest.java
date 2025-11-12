package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
@ActiveProfiles("test") // loads application-test.properties
@ExtendWith(DockerRequiredExtension.class)
public class DutyTypeRepositoryTest {

	@Autowired
	private DutyTypeRepository repository;

	@Test
	void testSaveAndFindById() {
		DutyType dutyType = TestEntityFactory.createDutyType();
		repository.save(dutyType);
		DutyType found = repository.findById(dutyType.getId()).orElse(null);

		assertThat(found).isNotNull();
		assertThat(found.getDutyTypeDescription()).isEqualTo(dutyType.getDutyTypeDescription());
		assertThat(found.getId().getDutyType()).isEqualTo(dutyType.getId().getDutyType());
		assertThat(found.getId().getDutyCode()).isEqualTo(dutyType.getId().getDutyCode());
	}

	@Test
	void testDelete() {
		DutyType dutyType = TestEntityFactory.createDutyType();
		repository.save(dutyType);
		
		repository.deleteById(dutyType.getId());
		assertThat(repository.findById(dutyType.getId())).isEmpty();
	}
}
