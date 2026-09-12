package com.biopet.repository;

public interface DashboardReportView {
    /**
     * @return number of active pets in the reported range
     */
    Long getMascotasActivas();
    /**
     * @return number of scheduled appointments in the reported range
     */
    Long getCitasProgramadas();
    /**
     * @return number of consultations in the reported range
     */
    Long getConsultasEnRango();
    /**
     * @return number of vaccine applications in the reported range
     */
    Long getVacunasEnRango();
    /**
     * @return number of pets without any consultation in the reported range
     */
    Long getMascotasSinConsulta();
}
