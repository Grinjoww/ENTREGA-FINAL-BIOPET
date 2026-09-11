package com.biopet.repository;

public interface DashboardReportView {
    Long getMascotasActivas();
    Long getCitasProgramadas();
    Long getConsultasEnRango();
    Long getVacunasEnRango();
    Long getMascotasSinConsulta();
}
