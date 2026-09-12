package com.biopet.repository;

public interface SpeciesSummary {
    /**
     * @return species name of the aggregated row
     */
    String getEspecie();
    /**
     * @return number of active pets of the species
     */
    Long getTotal();
}