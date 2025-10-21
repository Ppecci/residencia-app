package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;

public class PrescripcionDAO {

    /** DTO de lectura para la tabla de prescripciones activas. */
    public static class PrescView {
        public final int id;
        public final String medicamento;   // nombre
        public final String forma;         // comprimido, jarabe…
        public final String fuerza;        // 500mg…
        public final String dosis;         // "1 comp"
        public final String frecuencia;    // "cada 8h"
        public final String via;           // oral, IV…
        public final String inicio;        // start_date
        public final String fin;           // end_date (puede ser null/"" -> activa)
        public final String notas;

        public PrescView(int id, String medicamento, String forma, String fuerza,
                         String dosis, String frecuencia, String via,
                         String inicio, String fin, String notas) {
            this.id = id; this.medicamento = medicamento; this.forma = forma; this.fuerza = fuerza;
            this.dosis = dosis; this.frecuencia = frecuencia; this.via = via;
            this.inicio = inicio; this.fin = fin; this.notas = notas;
        }

        // getters para TableView
        public int getId() { return id; }
        public String getMedicamento() { return medicamento; }
        public String getForma() { return forma; }
        public String getFuerza() { return fuerza; }
        public String getDosis() { return dosis; }
        public String getFrecuencia() { return frecuencia; }
        public String getVia() { return via; }
        public String getInicio() { return inicio; }
        public String getFin() { return fin; }
        public String getNotas() { return notas; }
    }

    /** Lista prescripciones activas (end_date NULL o vacío) para un residente. */
    public List<PrescView> listarActivas(int residenteId) throws Exception {
        String sql = """
            SELECT p.id,
                   m.nombre    AS medicamento,
                   m.forma     AS forma,
                   m.fuerza    AS fuerza,
                   p.dosis, p.frecuencia, p.via,
                   p.start_date AS inicio,
                   p.end_date   AS fin,
                   p.notas
            FROM prescripciones p
            JOIN medicaciones m ON m.id = p.medicacion_id
            WHERE p.residente_id = ?
              AND (p.end_date IS NULL OR p.end_date = '')
            ORDER BY m.nombre
            """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescView> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new PrescView(
                        rs.getInt("id"),
                        rs.getString("medicamento"),
                        rs.getString("forma"),
                        rs.getString("fuerza"),
                        rs.getString("dosis"),
                        rs.getString("frecuencia"),
                        rs.getString("via"),
                        rs.getString("inicio"),
                        rs.getString("fin"),
                        rs.getString("notas")
                    ));
                }
                return out;
            }
        }
    }

    /** ¿Existe ya una prescripción ACTIVA del mismo medicamento para este residente? */
    public boolean existeActivaMismoMedicamento(int residenteId, int medicacionId) throws Exception {
        String sql = """
            SELECT 1
            FROM prescripciones
            WHERE residente_id = ? AND medicacion_id = ?
              AND (end_date IS NULL OR end_date = '')
            LIMIT 1
            """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, medicacionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Insertar nueva prescripción (activa: end_date = NULL). */
    public void insertar(int residenteId, int medicacionId,
                         String dosis, String frecuencia, String via,
                         String startDate, String notas) throws Exception {
        String sql = """
            INSERT INTO prescripciones
                (residente_id, medicacion_id, dosis, frecuencia, via, start_date, end_date, notas)
            VALUES (?,?,?,?,?,?,NULL,?)
            """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, medicacionId);
            ps.setString(3, dosis);
            ps.setString(4, frecuencia);
            ps.setString(5, via);
            ps.setString(6, startDate);
            ps.setString(7, notas);
            ps.executeUpdate();
        }
    }
    /** Finalizar prescripción (establecer end_date). */
    public void finalizar(int prescripcionId, String fechaFin) throws Exception {
    String sql = "UPDATE prescripciones SET end_date = ? WHERE id = ?";
    try (Connection c = ConexionBD.obtener();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, fechaFin);
        ps.setInt(2, prescripcionId);
        ps.executeUpdate();
    }
}
}

