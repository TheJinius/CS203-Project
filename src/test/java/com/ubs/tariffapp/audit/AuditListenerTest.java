package com.ubs.tariffapp.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.ubs.tariffapp.models.TariffSchedule;

@ExtendWith(MockitoExtension.class)
class AuditListenerTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AuditLogService auditLogService;

    private AuditListener auditListener;

    @BeforeEach
    void setUp() {
        auditListener = new AuditListener();
        auditListener.setApplicationContext(applicationContext);
    }

    @Test
    @DisplayName("Constructor with AuditLogService initializes successfully")
    void testConstructorWithService() {
        // Act
        AuditListener listener = new AuditListener(auditLogService);

        // Assert
        assertThat(listener).isNotNull();
    }

    @Test
    @DisplayName("Default constructor initializes successfully")
    void testDefaultConstructor() {
        // Act
        AuditListener listener = new AuditListener();

        // Assert
        assertThat(listener).isNotNull();
    }

    @Test
    @DisplayName("setApplicationContext sets context successfully")
    void testSetApplicationContext() {
        // Act
        auditListener.setApplicationContext(applicationContext);

        // Assert - verify context is set by testing postPersist behavior
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        
        TariffSchedule entity = new TariffSchedule();
        auditListener.postPersist(entity);
        
        verify(auditLogService, times(1)).logChange(entity, "INSERT");
    }

    @Test
    @DisplayName("postPersist logs INSERT when service is available")
    void testPostPersist_Success() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();

        // Act
        auditListener.postPersist(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "INSERT");
    }

    @Test
    @DisplayName("postPersist handles exception gracefully")
    void testPostPersist_ExceptionHandled() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();
        doThrow(new RuntimeException("Database error")).when(auditLogService).logChange(any(), eq("INSERT"));

        // Act - should not throw exception
        auditListener.postPersist(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "INSERT");
    }

    @Test
    @DisplayName("postPersist does nothing when service is not available")
    void testPostPersist_NoServiceAvailable() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class))
            .thenThrow(new NoSuchBeanDefinitionException("AuditLogService not found"));
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        auditListener.postPersist(entity);

        // Assert
        verify(auditLogService, never()).logChange(any(), any());
    }

    @Test
    @DisplayName("postPersist does nothing when ApplicationContext is null")
    void testPostPersist_NullApplicationContext() {
        // Arrange
        AuditListener listenerWithNullContext = new AuditListener();
        // Don't set ApplicationContext
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        listenerWithNullContext.postPersist(entity);

        // Assert - no verification needed, just ensure no exception
    }

    @Test
    @DisplayName("postUpdate logs UPDATE when service is available")
    void testPostUpdate_Success() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();

        // Act
        auditListener.postUpdate(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "UPDATE");
    }

    @Test
    @DisplayName("postUpdate handles exception gracefully")
    void testPostUpdate_ExceptionHandled() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();
        doThrow(new RuntimeException("Database error")).when(auditLogService).logChange(any(), eq("UPDATE"));

        // Act - should not throw exception
        auditListener.postUpdate(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "UPDATE");
    }

    @Test
    @DisplayName("postUpdate does nothing when service is not available")
    void testPostUpdate_NoServiceAvailable() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class))
            .thenThrow(new NoSuchBeanDefinitionException("AuditLogService not found"));
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        auditListener.postUpdate(entity);

        // Assert
        verify(auditLogService, never()).logChange(any(), any());
    }

    @Test
    @DisplayName("postUpdate does nothing when ApplicationContext is null")
    void testPostUpdate_NullApplicationContext() {
        // Arrange
        AuditListener listenerWithNullContext = new AuditListener();
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        listenerWithNullContext.postUpdate(entity);

        // Assert - no verification needed, just ensure no exception
    }

    @Test
    @DisplayName("postRemove logs DELETE when service is available")
    void testPostRemove_Success() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();

        // Act
        auditListener.postRemove(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "DELETE");
    }

    @Test
    @DisplayName("postRemove handles exception gracefully")
    void testPostRemove_ExceptionHandled() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class)).thenReturn(auditLogService);
        TariffSchedule entity = new TariffSchedule();
        doThrow(new RuntimeException("Database error")).when(auditLogService).logChange(any(), eq("DELETE"));

        // Act - should not throw exception
        auditListener.postRemove(entity);

        // Assert
        verify(auditLogService, times(1)).logChange(entity, "DELETE");
    }

    @Test
    @DisplayName("postRemove does nothing when service is not available")
    void testPostRemove_NoServiceAvailable() {
        // Arrange
        when(applicationContext.getBean(AuditLogService.class))
            .thenThrow(new NoSuchBeanDefinitionException("AuditLogService not found"));
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        auditListener.postRemove(entity);

        // Assert
        verify(auditLogService, never()).logChange(any(), any());
    }

    @Test
    @DisplayName("postRemove does nothing when ApplicationContext is null")
    void testPostRemove_NullApplicationContext() {
        // Arrange
        AuditListener listenerWithNullContext = new AuditListener();
        TariffSchedule entity = new TariffSchedule();

        // Act - should not throw exception
        listenerWithNullContext.postRemove(entity);

        // Assert - no verification needed, just ensure no exception
    }
}
