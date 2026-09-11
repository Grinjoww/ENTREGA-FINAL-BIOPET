package com.biopet.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobertura de rama de los callbacks @PrePersist de las entidades de dominio:
 * cada entidad decide, con un `if (campo == null) campo = valor`, si debe
 * autocompletar timestamps (y en Appointment/User, también un valor por defecto
 * de estado/rol) o respetar un valor ya asignado explícitamente. Ambas ramas
 * (campo nulo / campo ya asignado) se ejercitan aquí para cada entidad,
 * invocando directamente el método de paquete `prePersist()` (visible desde
 * esta clase de prueba porque vive en el mismo paquete `com.biopet.entity`).
 */
class EntityLifecycleCallbacksTest {

    @Test
    void citaPrePersistAutocompletaCuandoCamposSonNulos() {
        Appointment cita = new Appointment();

        cita.prePersist();

        assertNotNull(cita.getCreadoEn());
        assertNotNull(cita.getActualizadoEn());
        assertTrue(cita.isActivo());
        assertEquals(AppointmentStatus.PROGRAMADA, cita.getEstado());
    }

    @Test
    void citaPrePersistRespetaValoresYaAsignados() {
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-01-02T00:00:00Z");
        Appointment cita = new Appointment();
        cita.setCreadoEn(creado);
        cita.setActualizadoEn(actualizado);
        cita.setEstado(AppointmentStatus.COMPLETADA);

        cita.prePersist();

        assertEquals(creado, cita.getCreadoEn());
        assertEquals(actualizado, cita.getActualizadoEn());
        assertEquals(AppointmentStatus.COMPLETADA, cita.getEstado());
    }

    @Test
    void usuarioPrePersistAutocompletaCuandoCamposSonNulos() {
        User usuario = new User();

        usuario.prePersist();

        assertNotNull(usuario.getCreadoEn());
        assertNotNull(usuario.getActualizadoEn());
        assertTrue(usuario.isActivo());
        assertEquals(Rol.ROLE_DUENO, usuario.getRol());
    }

    @Test
    void usuarioPrePersistRespetaValoresYaAsignados() {
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-01-02T00:00:00Z");
        User usuario = new User();
        usuario.setCreadoEn(creado);
        usuario.setActualizadoEn(actualizado);
        usuario.setRol(Rol.ROLE_ADMIN);

        usuario.prePersist();

        assertEquals(creado, usuario.getCreadoEn());
        assertEquals(actualizado, usuario.getActualizadoEn());
        assertEquals(Rol.ROLE_ADMIN, usuario.getRol());
    }

    @Test
    void consultaPrePersistAutocompletaCuandoCamposSonNulos() {
        Consultation consulta = new Consultation();

        consulta.prePersist();

        assertNotNull(consulta.getCreadoEn());
        assertNotNull(consulta.getActualizadoEn());
        assertTrue(consulta.isActivo());
    }

    @Test
    void consultaPrePersistRespetaValoresYaAsignados() {
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-01-02T00:00:00Z");
        Consultation consulta = new Consultation();
        consulta.setCreadoEn(creado);
        consulta.setActualizadoEn(actualizado);

        consulta.prePersist();

        assertEquals(creado, consulta.getCreadoEn());
        assertEquals(actualizado, consulta.getActualizadoEn());
    }

    @Test
    void mascotaPrePersistAutocompletaCuandoCamposSonNulos() {
        Pet mascota = new Pet();

        mascota.prePersist();

        assertNotNull(mascota.getCreadoEn());
        assertNotNull(mascota.getActualizadoEn());
        assertTrue(mascota.isActivo());
    }

    @Test
    void mascotaPrePersistRespetaValoresYaAsignados() {
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-01-02T00:00:00Z");
        Pet mascota = new Pet();
        mascota.setCreadoEn(creado);
        mascota.setActualizadoEn(actualizado);

        mascota.prePersist();

        assertEquals(creado, mascota.getCreadoEn());
        assertEquals(actualizado, mascota.getActualizadoEn());
    }

    @Test
    void vacunaPrePersistAutocompletaCuandoCamposSonNulos() {
        Vaccine vacuna = new Vaccine();

        vacuna.prePersist();

        assertNotNull(vacuna.getCreadoEn());
        assertNotNull(vacuna.getActualizadoEn());
        assertTrue(vacuna.isActivo());
    }

    @Test
    void vacunaPrePersistRespetaValoresYaAsignados() {
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-01-02T00:00:00Z");
        Vaccine vacuna = new Vaccine();
        vacuna.setCreadoEn(creado);
        vacuna.setActualizadoEn(actualizado);

        vacuna.prePersist();

        assertEquals(creado, vacuna.getCreadoEn());
        assertEquals(actualizado, vacuna.getActualizadoEn());
    }

    @Test
    void citaPreUpdateActualizaMarcaDeTiempo() {
        Appointment cita = new Appointment();
        cita.setActualizadoEn(Instant.parse("2020-01-01T00:00:00Z"));

        cita.preUpdate();

        assertTrue(cita.getActualizadoEn().isAfter(Instant.parse("2020-01-01T00:00:00Z")));
    }
}
