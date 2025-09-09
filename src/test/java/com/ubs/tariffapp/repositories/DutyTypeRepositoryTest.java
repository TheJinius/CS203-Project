package com.ubs.tariffapp.repositories;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestConstants;

@DataJpaTest
public class DutyTypeRepositoryTest {
	private static final String DUTY_TYPE = TestConstants.DUTY_TYPE;
	private static final String DUTY_CODE = TestConstants.DUTY_CODE;
	private static final String DESCRIPTION = TestConstants.DUTY_TYPE_DESCRIPTION;
	private static final List<TariffSchedule> TARIFF_SCHEDULES = TestConstants.DUTY_TYPE_TARIFF_SCHEDULES;

	@Autowired
	private DutyTypeRepository repository;

	@Test
	void testSaveAndFindById() {
		DutyTypeId id = new DutyTypeId(DUTY_TYPE, DUTY_CODE);
		DutyType dutyType = new DutyType(id, DESCRIPTION, TARIFF_SCHEDULES);
		repository.save(dutyType);
		DutyType found = repository.findById(id).orElse(null);
		assertThat(found).isNotNull();
		assertThat(found.getDutyTypeDescription()).isEqualTo(DESCRIPTION);
		assertThat(found.getId().getDutyType()).isEqualTo(DUTY_TYPE);
		assertThat(found.getId().getDutyCode()).isEqualTo(DUTY_CODE);
	}

	@Test
	void testDelete() {
		DutyTypeId id = new DutyTypeId(DUTY_TYPE, DUTY_CODE);
		DutyType dutyType = new DutyType(id, DESCRIPTION, TARIFF_SCHEDULES);
		repository.save(dutyType);
		repository.deleteById(id);
		assertThat(repository.findById(id)).isEmpty();
	}
}
