package com.ubs.tariffapp.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ubs.tariffapp.models.TariffSchedule;

class AuditEventTest {

    @Test
    @DisplayName("Constructor initializes event with entity and changeType")
    void testConstructor() {
        // Arrange
        TariffSchedule entity = new TariffSchedule();
        String changeType = "INSERT";

        // Act
        AuditEvent event = new AuditEvent(entity, changeType);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getEntity()).isEqualTo(entity);
        assertThat(event.getChangeType()).isEqualTo(changeType);
    }

    @Test
    @DisplayName("getEntity returns the correct entity")
    void testGetEntity() {
        // Arrange
        TariffSchedule entity = new TariffSchedule();
        entity.setTariffId(12345);
        AuditEvent event = new AuditEvent(entity, "UPDATE");

        // Act
        Object result = event.getEntity();

        // Assert
        assertThat(result).isEqualTo(entity);
        assertThat(result).isInstanceOf(TariffSchedule.class);
        assertThat(((TariffSchedule) result).getTariffId()).isEqualTo(12345);
    }

    @Test
    @DisplayName("getChangeType returns the correct change type")
    void testGetChangeType() {
        // Arrange
        TariffSchedule entity = new TariffSchedule();
        AuditEvent event = new AuditEvent(entity, "DELETE");

        // Act
        String result = event.getChangeType();

        // Assert
        assertThat(result).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("Event handles null entity")
    void testNullEntity() {
        // Act
        AuditEvent event = new AuditEvent(null, "INSERT");

        // Assert
        assertThat(event.getEntity()).isNull();
        assertThat(event.getChangeType()).isEqualTo("INSERT");
    }

    @Test
    @DisplayName("Event handles null changeType")
    void testNullChangeType() {
        // Arrange
        TariffSchedule entity = new TariffSchedule();

        // Act
        AuditEvent event = new AuditEvent(entity, null);

        // Assert
        assertThat(event.getEntity()).isEqualTo(entity);
        assertThat(event.getChangeType()).isNull();
    }

    @Test
    @DisplayName("Event handles different entity types")
    void testDifferentEntityTypes() {
        // Arrange
        String stringEntity = "Test Entity";
        Integer integerEntity = 42;

        // Act
        AuditEvent stringEvent = new AuditEvent(stringEntity, "INSERT");
        AuditEvent integerEvent = new AuditEvent(integerEntity, "UPDATE");

        // Assert
        assertThat(stringEvent.getEntity()).isEqualTo(stringEntity);
        assertThat(stringEvent.getEntity()).isInstanceOf(String.class);
        
        assertThat(integerEvent.getEntity()).isEqualTo(integerEntity);
        assertThat(integerEvent.getEntity()).isInstanceOf(Integer.class);
    }

    @Test
    @DisplayName("Multiple events maintain separate state")
    void testMultipleEvents() {
        // Arrange
        TariffSchedule entity1 = new TariffSchedule();
        entity1.setTariffId(100);
        
        TariffSchedule entity2 = new TariffSchedule();
        entity2.setTariffId(200);

        // Act
        AuditEvent event1 = new AuditEvent(entity1, "INSERT");
        AuditEvent event2 = new AuditEvent(entity2, "UPDATE");

        // Assert
        assertThat(event1.getEntity()).isNotEqualTo(event2.getEntity());
        assertThat(event1.getChangeType()).isNotEqualTo(event2.getChangeType());
        
        assertThat(((TariffSchedule) event1.getEntity()).getTariffId()).isEqualTo(100);
        assertThat(((TariffSchedule) event2.getEntity()).getTariffId()).isEqualTo(200);
    }
}
