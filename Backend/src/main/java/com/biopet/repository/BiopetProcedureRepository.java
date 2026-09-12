package com.biopet.repository;

import com.biopet.entity.Pet;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio dedicado a la invocacion formal (JPA) de las rutinas
 * almacenadas de BIOPET definidas en db/procs/ y replicadas en la migracion
 * V5. No expone metodos CRUD: solo las 6 invocaciones (F02), las 6 con
 * {@code @Procedure}.
 *
 * <p>Spring Data JPA / Hibernate generan siempre una sentencia {@code CALL}
 * para {@code @Procedure}, y PostgreSQL solo acepta {@code CALL} sobre
 * objetos {@code PROCEDURE} (nunca sobre {@code FUNCTION}, ni siquiera con
 * parametros {@code OUT}: verificacion real F03, PostgreSQL responde
 * "... is not a procedure. Hint: To call a function, use SELECT"). Por eso
 * las 4 rutinas {@code fn_*}, que originalmente eran {@code FUNCTION}
 * ({@code RETURNS TABLE} / OUT escalar directo, invocadas con
 * {@code @Query(nativeQuery = true)}), se reclasificaron a {@code PROCEDURE}
 * conservando su nombre {@code fn_...}.
 *
 * <p>Las 3 rutinas que devuelven un conjunto de filas
 * ({@code speciesSummary}, {@code petClinicalHistory},
 * {@code dashboardReport}) exponen un unico {@code OUT refcursor} (un
 * {@code PROCEDURE} no admite {@code RETURNS TABLE}) y se invocan con
 * {@code @Procedure(name = "...")} referenciando un
 * {@code @NamedStoredProcedureQuery} declarado en {@link Pet} con
 * {@code ParameterMode.REF_CURSOR} explicito: probar {@code @Procedure} sin
 * un {@code @NamedStoredProcedureQuery} (parametros auto-derivados de los
 * metadatos JDBC) genera una sentencia {@code CALL} que omite el
 * placeholder del parametro {@code refcursor}, que PostgreSQL rechaza con
 * {@code procedure ... does not exist} (verificacion real F03) por
 * discordancia de aridad. El cursor que abre {@code OPEN p_cursor FOR
 * SELECT ...} solo es legible dentro de la misma transaccion en la que se
 * abre; por eso estos 3 metodos NO declaran su propio
 * {@code @Transactional} (una transaccion iniciada por el propio proxy del
 * repositorio no es reconocida como "transaccion circundante" por Spring
 * Data y falla con {@code InvalidDataAccessApiUsageException}) sino que
 * dependen de que el codigo llamador ya este dentro de una transaccion —
 * como ya lo esta {@code PetService.speciesSummary}
 * (@Transactional(readOnly = true)), y como declaran explicitamente los
 * tests de integracion que los ejercitan.
 *
 * <p>{@code nextRecordNumber} ya devolvia un escalar, por lo que su
 * {@code PROCEDURE} usa {@code OUT p_codigo VARCHAR} directo (sin
 * refcursor, sin @NamedStoredProcedureQuery, sin requisito de transaccion
 * circundante), igual que los 2 {@code sp_*} (que ya eran {@code PROCEDURE}
 * y no cambiaron).
 */
public interface BiopetProcedureRepository extends Repository<Pet, Long> {

    /**
     * Summarizes active pets grouped by species via stored procedure.
     *
     * @param duenioId identifier of the owning user scoping the summary
     * @return one row per species with its pet count
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(name = "fn_resumen_mascotas_por_especie")
    List<SpeciesSummary> speciesSummary(Long duenioId);

    /**
     * Reads the clinical history of one pet via stored procedure.
     *
     * @param mascotaId identifier of the pet
     * @return clinical history rows of the pet
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(name = "fn_historial_clinico_mascota")
    List<ClinicalHistoryView> petClinicalHistory(Long mascotaId);

    /**
     * Reads the dashboard report for a date range via stored procedure.
     *
     * @param desde start of the reported range
     * @param hasta end of the reported range
     * @return dashboard report rows for the range
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(name = "fn_reporte_dashboard")
    List<DashboardReportView> dashboardReport(LocalDate desde, LocalDate hasta);

    /**
     * Generates the next sequential record code via stored procedure.
     *
     * @param prefijo code prefix qualifying the sequence
     * @return the next code of the sequence
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(procedureName = "fn_siguiente_numero_ficha", outputParameterName = "p_codigo")
    String nextRecordNumber(@Param("p_prefijo") String prefijo);

    /**
     * Bulk-updates the status of appointments via stored procedure.
     *
     * @param veterinarioId identifier of the veterinarian owning the appointments
     * @param estadoAnterior only appointments currently in this status are updated
     * @param estadoNuevo status assigned to the matching appointments
     * @param fechaLimite only appointments up to this instant are updated
     * @return number of affected appointments
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(procedureName = "sp_actualizar_estado_citas_masivas", outputParameterName = "p_afectadas")
    Long bulkUpdateAppointmentStatus(@Param("p_veterinario_id") Long veterinarioId,
                                      @Param("p_estado_anterior") String estadoAnterior,
                                      @Param("p_estado_nuevo") String estadoNuevo,
                                      @Param("p_fecha_limite") Instant fechaLimite);

    /**
     * Registers a validated clinical consultation via stored procedure.
     *
     * @param mascotaId identifier of the examined pet
     * @param veterinarioId identifier of the responsible veterinarian
     * @param motivo reason for the visit
     * @param diagnostico recorded diagnosis
     * @param tratamiento prescribed treatment
     * @param observaciones additional notes
     * @return identifier of the created consultation
     * @throws org.springframework.dao.DataAccessException if the call fails
     */
    @Procedure(procedureName = "sp_registrar_consulta_validada", outputParameterName = "p_consulta_id")
    Long registerValidatedConsultation(@Param("p_mascota_id") Long mascotaId,
                                   @Param("p_veterinario_id") Long veterinarioId,
                                   @Param("p_motivo") String motivo,
                                   @Param("p_diagnostico") String diagnostico,
                                   @Param("p_tratamiento") String tratamiento,
                                   @Param("p_observaciones") String observaciones);
}
