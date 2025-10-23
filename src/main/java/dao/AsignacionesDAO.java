package dao;

import bd.ConexionBD;
import modelo.AsignacionVista;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AsignacionesDAO {

    public List<AsignacionVista> listarPorTrabajador(int idTrabajador, boolean soloVigentes) throws Exception {
        String sql = """
           SELECT a.id AS asignacion_id,
                  r.id AS residente_id,
                  r.nombre || ' ' || IFNULL(r.apellidos,'') AS residente,
                  a.start_date AS inicio,
                  a.end_date   AS fin,
                  a.notas
           FROM asignacion_trabajador a
           JOIN residentes r ON r.id = a.residente_id
           WHERE a.trabajador_id = ?
        """;
        if (soloVigentes) {
            sql += " AND (a.end_date IS NULL OR date(a.end_date) > date('now')) ";
        }
        sql += " ORDER BY r.apellidos, r.nombre";

        List<AsignacionVista> out = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTrabajador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AsignacionVista v = new AsignacionVista();
                    v.setIdAsignacion(rs.getInt("asignacion_id"));
                    v.setIdResidente(rs.getInt("residente_id"));
                    v.setResidente(rs.getString("residente"));
                    v.setInicio(rs.getString("inicio"));
                    v.setFin(rs.getString("fin"));
                    v.setNotas(rs.getString("notas"));
                    out.add(v);
                }
            }
        }
        return out;
    }
}
