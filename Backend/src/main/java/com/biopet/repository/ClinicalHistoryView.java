package com.biopet.repository;

import java.time.Instant;
import java.time.LocalDate;

public interface ClinicalHistoryView {
    /**
     * @return name of the pet this history belongs to
     */
    String getMascota();
    /**
     * @return species of the pet
     */
    String getEspecie();
    /**
     * @return breed of the pet
     */
    String getRaza();
    /**
     * @return name of the owning user
     */
    String getDuenio();
    /**
     * @return number of recorded consultations
     */
    Long getNConsultas();
    /**
     * @return instant of the most recent consultation, when any exists
     */
    Instant getUltimaConsulta();
    /**
     * @return description of the most recent vaccine, when any exists
     */
    String getUltimaVacuna();
    /**
     * @return scheduled date of the next vaccine, when any exists
     */
    LocalDate getProximaVacuna();
    /**
     * @return number of recorded appointments
     */
    Long getNCitas();
}
