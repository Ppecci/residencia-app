package modelo;

public class PrescripcionVista {
    private final Integer id;
    private final Integer residenteId;
    private final Integer medicacionId;
    private final String  medicacionNombre; // m.nombre + (forma/fuerza)
    private final String  dosis;
    private final String  frecuencia;
    private final String  via;
    private final String  startDate;
    private final String  endDate;          // null si activa
    private final String  notas;

    public PrescripcionVista(Integer id, Integer residenteId, Integer medicacionId, String medicacionNombre,
                             String dosis, String frecuencia, String via,
                             String startDate, String endDate, String notas) {
        this.id = id;
        this.residenteId = residenteId;
        this.medicacionId = medicacionId;
        this.medicacionNombre = medicacionNombre;
        this.dosis = dosis;
        this.frecuencia = frecuencia;
        this.via = via;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notas = notas;
    }

    public Integer getId() { return id; }
    public Integer getResidenteId() { return residenteId; }
    public Integer getMedicacionId() { return medicacionId; }
    public String  getMedicacionNombre() { return medicacionNombre; }
    public String  getDosis() { return dosis; }
    public String  getFrecuencia() { return frecuencia; }
    public String  getVia() { return via; }
    public String  getStartDate() { return startDate; }
    public String  getEndDate() { return endDate; }
    public String  getNotas() { return notas; }

    public boolean isActiva() { return endDate == null || endDate.isBlank(); }
}
