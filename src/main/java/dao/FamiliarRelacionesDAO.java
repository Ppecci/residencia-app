package dao;

import bd.ConexionBD;
import modelo.FamiliarResidenteVista;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FamiliarRelacionesDAO {

    public List<FamiliarResidenteVista> listarPorFamiliar(int idFamiliar) throws Exception {
        String sql = """
            SELECT rf.id AS rel_id,
                   r.id  AS residente_id,
                   r.nombre || ' ' || IFNULL(r.apellidos,'') AS residente,
                   rf.parentesco
            FROM residente_familiar rf
            JOIN residentes r ON r.id = rf.residente_id
            WHERE rf.familiar_id = ?
            ORDER BY r.apellidos, r.nombre
        """;
        List<FamiliarResidenteVista> out = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFamiliar);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FamiliarResidenteVista v = new FamiliarResidenteVista();
                    v.setIdRelacion(rs.getInt("rel_id"));
                    v.setIdResidente(rs.getInt("residente_id"));
                    v.setResidente(rs.getString("residente"));
                    v.setParentesco(rs.getString("parentesco"));
                    out.add(v);
                }
            }
        }
        return out;
    }
}
