/*
 * Licensed under the GPL License.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */
package backingbeans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.HibernateDao;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import model.PerfilCampos;
import model.PerfilesCapas;
import servicios.CamposModel;
import servicios.CapasModel;
import servicios.ConsultaModel;
import servicios.PerfilModel;
import servicios.ResultadoBusqueda;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import dao.DaoAnubis;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import model.CapaFiltro;
import model.PerfilBase;
import model.PerfilPlugins;
import model.Perfiles;
import org.postgresql.geometric.PGpoint;
import servicios.BasesModel;
import servicios.ResultadoBusquedaPunto;
import utils.SendEmail;

/**
 *
 * @author Camila.Riveron
 */
@ManagedBean(name = "servicios")
@ViewScoped
public class ServiciosBean {

    
    public String getBusquedaPorCapas() throws Exception {
        ArrayList<ResultadoBusqueda> salida = new ArrayList<>();
        GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
        Gson gson = gsonBuilder.create();

        try {

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String camposJson = (String) request.getParameter("campos");

            ConsultaModel consulta = (ConsultaModel) gson.fromJson(camposJson, ConsultaModel.class);
            List<String> props = new ArrayList<>();
            List<String> etiquetas = new ArrayList();
            List r = HibernateDao.busqueda(consulta, props, etiquetas);

            boolean primero = true;
            String jsonSalida = "{ 'error': 0, 'capa': '" + consulta.getConsultas().get(0).getNombreCapa() + "', 'resultado': {";

            int iter = 0;
            for (Object o : r) {
                Object[] ob = (Object[]) o;
                //Integer id = (Integer) ob[0];
                //Geometria
                Geometry geom = (Geometry) ob[0];
                //Punto
                Geometry geom2 = (Geometry) ob[1];

                Point p = (Point) geom2;
                jsonSalida += primero ? "" : " ,";
                jsonSalida += iter + " : {";
                jsonSalida += "'cp': " + DaoAnubis.getCP(geom) + ", ";
                jsonSalida += "'x': " + geom2.getCoordinate().x + ", ";
                jsonSalida += "'y': " + geom2.getCoordinate().y + ", ";

                jsonSalida += "'propiedades': {";
                if (geom instanceof MultiPolygon) {
                    jsonSalida += "'color':'azul' ";
                } else if (geom instanceof Point) {
                    jsonSalida += "'color':'rojo' ";
                } else {
                    jsonSalida += "'color':'rosa' ";
                }
                String etiqueta = "";
                for (int i = 2; i < ob.length; i++) {
                    jsonSalida += " , '" + props.get(i - 2) + "' : '" + (String) ob[i] + "'";
                    if (etiquetas.contains(props.get(i - 2))) {
                        if (!etiqueta.equals("")) {
                            etiqueta += ", ";
                        }
                        etiqueta += (String) ob[i];
                    }
                }

                jsonSalida += " } , ";

                jsonSalida += "'nombre': '" + etiqueta + "', ";

                jsonSalida += "'capa': " + getGeometriaJSON(geom) + "}";

                primero = false;
                iter++;
            }
            jsonSalida += "}}";
            DaoAnubis.closeConnection();

            //System.out.println(jsonSalida);
            return jsonSalida;
        } catch (Exception ex) {
            Logger.getLogger(ServiciosBean.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

    }

    public String getsendEmail() {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String nombre = (String) request.getParameter("nombreEmail");
        String emailDest = (String) request.getParameter("Email");
        String mensaje = (String) request.getParameter("mensajeEmail");
        String link = (String) request.getParameter("linkVista");

        String html = "<html><h3 style='color:#384F8F; font-size: medium;'>Mensaje enviado a través Isis Visualizador</h3><h3 style='color:#384F8F; font-size: medium;'>Remitente</h3><p style='color:#4E4F5C;'>" + nombre + " (" + emailDest + ")</p><h3 style='color:#384F8F; font-size: medium'>Mensaje</h3><p style='color:#4E4F5C;'>" + mensaje + "</p><h3 style='color:#384F8F; font-size: medium'>Link a la vista en el mapa:</h3> <br/> <p style='color:#4E4F5C;'>" + link + "</p></html>";

        try {
            SendEmail e = new SendEmail();
            List<String> tos = new LinkedList<>();
            tos.add("geomatica@correo.com.uy");
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Date d = new Date();
            e.sendMail("[Isis Visualizador] " + df.format(d) + "", html, tos, null, null, null, false);
            return "OK";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error";
        }
    }

    

    public String getbusquedaNumeroCuentaUTE() throws Exception {


        ArrayList<ResultadoBusqueda> salida = new ArrayList<>();
        GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
        Gson gson = gsonBuilder.create();

        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String jsonSalida = "";
            Boolean esNIS = Boolean.valueOf(request.getParameter("EsNIS"));
            int NumeroNIS = Integer.parseInt(request.getParameter("NumeroNIS"));
            String NumeroCuentaUTE = request.getParameter("NumeroCuentaUTEValor");
            ResultadoBusquedaPunto rs;
            if (esNIS) {
                rs = DaoAnubis.buscarDireccionNumeroCuentaUTE("", NumeroNIS, true);
            } else {
                rs = DaoAnubis.buscarDireccionNumeroCuentaUTE(NumeroCuentaUTE, 0, false);
            }
            DaoAnubis.transformar(rs, 31981, 4326);
            rs.setCp(DaoAnubis.getCP(rs.getPunto()));
            jsonSalida += gson.toJson(rs);
            System.out.println(jsonSalida);
            return jsonSalida;
        } catch (Exception ex) {
            Logger.getLogger(ServiciosBean.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

    }

    
    public String getguardarObsNumeroCuentaUTE() throws Exception {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String obs = URLDecoder.decode((String) request.getParameter("NumeroCuentaUTEObs"), "UTF-8");
        String NumeroCuentaUTE = request.getParameter("NumeroCuentaUTENum");
        Boolean esNIS = request.getParameter("EsNIS").equals("true") ? true : false;
        String res;
        if (esNIS) {
            res = DaoAnubis.guardarObsNumeroCuentaUTE("", Integer.parseInt(NumeroCuentaUTE), obs, esNIS);
        } else {
            res = DaoAnubis.guardarObsNumeroCuentaUTE(NumeroCuentaUTE, 0, obs, esNIS);
        }

        return res;
    }

    
    public String getPropiedadesJSON(HashMap<String, String> propiedades) {
        String salida = "{";

        for (String k : propiedades.keySet()) {
            if (!salida.equals("{")) {
                salida += ",";
            }
            salida += k + ": '" + propiedades.get(k) + "'";
        }

        salida += "}";
        return salida;
    }

    public String getGeometriaJSON(Geometry geometria) {
        if (geometria == null) {
            return "null";
        }

        String salida = "{tipo: '" + geometria.getGeometryType() + "',";
        salida += "srid: " + geometria.getSRID() + ",";
        salida += "geometrias: [";
        if (geometria instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) geometria;
            int i = 1;

            for (int y = 0; y < mp.getNumGeometries(); y++) {
                Polygon p = (Polygon) mp.getGeometryN(y);
                if (i != 1) {
                    salida += ",";
                }
                GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
                Gson gson = gsonBuilder.create();
                salida += gson.toJson(p.getExteriorRing().getCoordinates());

                i++;
            }

        } else if (geometria instanceof MultiLineString) {

            GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
            Gson gson = gsonBuilder.create();
            salida += gson.toJson(geometria.getCoordinates());
        } else if (geometria instanceof Point) {
            Point pp = (Point) geometria;
            salida += "{x:" + pp.getCoordinate().x + ",y:" + pp.getCoordinate().y + "}";
        } else {
            System.out.println("TIPO NO SOPORTADO: " + geometria.getGeometryType());
            return "null";
        }
        salida += "]}";

        return salida;
    }

    public String getCapasJson() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String nombrePerfil = (String) request.getParameter("perfil");

        try {
            nombrePerfil = URLDecoder.decode(nombrePerfil, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HibernateDao.class.getName()).log(Level.SEVERE, null, ex);
            nombrePerfil = "Default";
        }

        List<PerfilesCapas> capas = HibernateDao.getPerfilByName(nombrePerfil);
        Perfiles perf = HibernateDao.getPerfilPorNombre(nombrePerfil);
        Collections.sort((List) capas);
        Collections.reverse((List) capas);

        List<PerfilBase> bases = HibernateDao.getPerfilBaseByName(nombrePerfil);
        Collections.sort((List) bases);
        //Collections.reverse((List) bases);

        PerfilModel perfil = new PerfilModel();
        perfil.setNombrePerfil(nombrePerfil);
        List<CapasModel> capasConvertidas = convertCapaModel(capas);
        List<BasesModel> basesConvertidas = convertBasesModel(bases);
        perfil.setCapas(capasConvertidas);
        perfil.setBases(basesConvertidas);

        Gson gson;
        gson = new Gson();
        String jsonRes = gson.toJson(perfil);
        System.out.println(jsonRes);
        return jsonRes;
    }

    public String getcoordenadasTransformadas() {
        String res = "";
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        Double coordX = Double.parseDouble(request.getParameter("coord_x"));
        Double coordY = Double.parseDouble(request.getParameter("coord_y"));
        String srid = (String) request.getParameter("srid");

        PGpoint p = DaoAnubis.getCoordenadasTransformadas(coordX, coordY, srid);
        if (p != null) {
            res += "{error: false, punto_x: " + p.x + ", punto_y: " + p.y + "}";
        } else {
            res += "{error: true, mensaje: 'Ocurrió un error al procesar el punto. Verifique que el Sistema de Referencia sea correcto.'}";
        }
        return res;
    }

    public String getPluginsJson() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String nombrePerfil = (String) request.getParameter("perfil");

        try {
            nombrePerfil = URLDecoder.decode(nombrePerfil, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HibernateDao.class.getName()).log(Level.SEVERE, null, ex);
            nombrePerfil = "Default";
        }

        Perfiles p = HibernateDao.getPerfilPorNombre(nombrePerfil);
        PerfilModel perfil = new PerfilModel();
        perfil.setNombrePerfil(nombrePerfil);
        perfil.setCapas(new ArrayList());
        perfil.setPlugins(new ArrayList());
        for (PerfilPlugins plugin : p.getPerfilPluginsList()) {
            perfil.getPlugins().add(plugin.getPluginId().getJs());
        }

        Gson gson;
        gson = new Gson();
        String jsonRes = gson.toJson(perfil);

        return jsonRes;
    }

    

    public String getListaDeValores() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String tabla = (String) request.getParameter("tabla");
        String campo = (String) request.getParameter("campo");

        try {
            tabla = URLDecoder.decode(tabla, "UTF-8");
            campo = URLDecoder.decode(campo, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HibernateDao.class.getName()).log(Level.SEVERE, null, ex);
        }

        List<String> lista = new ArrayList<>();

        lista = DaoAnubis.getListadeValores(tabla, campo);

        Gson gson;
        gson = new Gson();
        String jsonRes = gson.toJson(lista);

        return jsonRes;
    }

    private List<CapasModel> convertCapaModel(List<PerfilesCapas> capas) {
        List<CapasModel> capasConv = new ArrayList<CapasModel>();
        for (PerfilesCapas capa : capas) {
            CapasModel capaC = new CapasModel();
            capaC.setNombre(capa.getCapaId().getNombre());
            capaC.setTabla(capa.getCapaId().getTabla());
            capaC.setVisible(capa.getVisible());
            capaC.setSoloBuscable(capa.getSoloBuscable());
            capaC.setGrupo(capa.getGrupo());
            capaC.setMetadato(capa.getCapaId().getMetadato());
            capaC.setEscalaMaxima(capa.getCapaId().getEscalaMaxima());
            capaC.setEscalaMinima(capa.getCapaId().getEscalaMinima());
            capaC.setBuscable(capa.getBuscable());
            
            String template = capa.getTemplatePopUp();
            if (template == null || template.trim().equals("")){
                template = capa.getCapaId().getTemplatePopUp(); // Si el perfil no tiene template asociado se le setea el por defecto de la capa. 
            }
            capaC.setTemplatePopUp(template);
            List<CamposModel> campos = new ArrayList<CamposModel>();
            List<String> filtros = new ArrayList<>();
            
            for(CapaFiltro filtro : capa.getCapaFiltroList()){
                filtros.add(filtro.getIdCapaFiltro().getNombreCapa());
            }
            capaC.setFiltros(filtros);

            for (PerfilCampos campo : capa.getPerfilCamposList()) {
                CamposModel campoC = new CamposModel();
                campoC.setEtiquetaResultado(campo.getEtiquetaResultado());
                campoC.setCriterioBusqueda(campo.getCriterioBusqueda());
                campoC.setEtiqueta(campo.getEtiqueta());
                campoC.setNombreColumna(campo.getNombreColumna());
                campos.add(campoC);
                if (campo.getCriterioBusqueda().equals("Lista")) {
                    List<String> lista = DaoAnubis.getListadeValores(capaC.getTabla(), campoC.getNombreColumna());
                    String combo = "<select id='combo_" + capaC.getTabla() + "_" + campoC.getEtiqueta() + "'> <option value='null'>[Seleccione un valor]</option>";
                    for (String l : lista) {
                        if (l!=null) {
                            combo += "<option value='" + l + "'>" + l + "</option>";
                        }                        
                    }
                    combo += "</select>";
                    campoC.setLista(combo);
                }
            }

            capaC.setCampos(campos);
            capasConv.add(capaC);
        }

        return capasConv;
    }

    private List<BasesModel> convertBasesModel(List<PerfilBase> bases) {
        List<BasesModel> basesConv = new ArrayList<>();
        for (PerfilBase base : bases) {
            BasesModel baseC = new BasesModel();
            baseC.setEtiqueta(base.getBaseId().getEtiqueta());
            baseC.setCapas(base.getBaseId().getTablas());
            basesConv.add(baseC);
        }
        return basesConv;
    }
}
