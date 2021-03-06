/*
 * SemanticWebBuilder es una plataforma para el desarrollo de portales y aplicaciones de integración,
 * colaboración y conocimiento, que gracias al uso de tecnología semántica puede generar contextos de
 * información alrededor de algún tema de interés o bien integrar información y aplicaciones de diferentes
 * fuentes, donde a la información se le asigna un significado, de forma que pueda ser interpretada y
 * procesada por personas y/o sistemas, es una creación original del Fondo de Información y Documentación
 * para la Industria INFOTEC, cuyo registro se encuentra actualmente en trámite.
 * 
 * INFOTEC pone a su disposición la herramienta SemanticWebBuilder a través de su licenciamiento abierto al público (‘open source’),
 * en virtud del cual, usted podrá usarlo en las mismas condiciones con que INFOTEC lo ha diseñado y puesto a su disposición;
 * aprender de él; distribuirlo a terceros; acceder a su código fuente y modificarlo, y combinarlo o enlazarlo con otro software,
 * todo ello de conformidad con los términos y condiciones de la LICENCIA ABIERTA AL PÚBLICO que otorga INFOTEC para la utilización
 * del SemanticWebBuilder 4.0.
 * 
 * INFOTEC no otorga garantía sobre SemanticWebBuilder, de ninguna especie y naturaleza, ni implícita ni explícita,
 * siendo usted completamente responsable de la utilización que le dé y asumiendo la totalidad de los riesgos que puedan derivar
 * de la misma.
 * 
 * Si usted tiene cualquier duda o comentario sobre SemanticWebBuilder, INFOTEC pone a su disposición la siguiente
 * dirección electrónica:
 * http://www.semanticwebbuilder.org
 */

package mx.org.cedn.avisosconagua.engine.processors;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mx.org.cedn.avisosconagua.engine.Processor;
import mx.org.cedn.avisosconagua.mongo.MongoInterface;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author serch
 */
public class Pronostico implements Processor {
    private static final int MAX_SIZE = 3 * 1024 * 1024;

    @Override
    public void invokeForm(HttpServletRequest request, HttpServletResponse response, BasicDBObject data, String parts[]) throws ServletException, IOException {
        HashMap<String, String> datos = new HashMap<>();
        String prevIssue = null;
        if (null != data) {
            for (String key : data.keySet()) {
                datos.put(key, data.getString(key));
                //System.out.println("colocando: "+key+" : "+datos.get(key));
            }
        }
        
        //Put nhcLinks in map
        BasicDBObject advice = MongoInterface.getInstance().getAdvice((String)request.getSession(true).getAttribute("internalId"));
        if (null != advice) {
            //Get nhcLinks
            BasicDBObject section = (BasicDBObject) advice.get("precapture");
            if (null != section) {
                datos.put("nhcForecastLink", section.getString("nhcForecastLink"));
                datos.put("nhcPublicLink", section.getString("nhcPublicLink"));
                prevIssue = section.getString("previousIssue");
            }
        }
        
        //Advice without pronostico saved and for tracking
        if (advice != null && advice.get("pronostico") == null) {
            if (prevIssue != null) {
                BasicDBObject previous = MongoInterface.getInstance().getAdvice(prevIssue);
                if (previous != null) {
                    //System.out.println("Putting previous data from "+prevIssue);
                    BasicDBObject forecastSection = (BasicDBObject) previous.get("pronostico");
                    if (forecastSection != null) {
                        //Set current values to previous values
                        datos.put("issueSateliteLocationImgFooter", forecastSection.getString("issueSateliteLocationImgFooter", ""));
                    }
                }
            }
        }
        
        request.setAttribute("data", datos);
        request.setAttribute("bulletinType", parts[2]);
        String url = "/jsp/forecast.jsp";
        RequestDispatcher rd = request.getRequestDispatcher(url);
        rd.forward(request, response);
    }

    @Override
    public void processForm(HttpServletRequest request, String[] parts, String currentId) throws ServletException, IOException {
        HashMap<String, String> parametros = new HashMap<>();
        if (ServletFileUpload.isMultipartContent(request)) {
            try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(MAX_SIZE);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(MAX_SIZE);
            List<FileItem> items = upload.parseRequest(request);
            String filename = null;
            for (FileItem item : items) {
                if (!item.isFormField() && item.getSize()>0) {
                    filename = processUploadedFile(item, currentId);
                    parametros.put(item.getFieldName(), filename);
                } else {
                    //System.out.println("item:" + item.getFieldName() + "=" + item.getString());
                    parametros.put(item.getFieldName(), new String(item.getString().getBytes("ISO8859-1"), "UTF-8"));
                }
            }
            } catch (FileUploadException fue){
                fue.printStackTrace();
            }
        } else {
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                try {
                    parametros.put(entry.getKey(), new String(request.getParameter(entry.getKey()).getBytes("ISO8859-1"), "UTF-8"));
                } catch (UnsupportedEncodingException ue) {
                    //No debe llegar a este punto
                    assert false;
                }
            }
        }
        BasicDBObject anterior = (BasicDBObject)MongoInterface.getInstance().getAdvice(currentId).get(parts[3]);
        procesaImagen(parametros, anterior);
        MongoInterface.getInstance().savePlainData(currentId, parts[3], parametros);
    }
    
    private String processUploadedFile(FileItem item, String currentId) throws IOException {
        GridFS gridfs = MongoInterface.getInstance().getImagesFS();
        GridFSInputFile gfsFile = gridfs.createFile(item.getInputStream());
        String filename = currentId + ":" + item.getFieldName() + "_" + item.getName();
        gfsFile.setFilename(filename);
        gfsFile.setContentType(item.getContentType());
        gfsFile.save();
        return filename;
    }
    
    private void procesaImagen(HashMap<String, String> parametros, BasicDBObject anterior) {
        if (null == parametros.get("issueSateliteLocationImg")||"".equals(parametros.get("issueSateliteLocationImg").trim())){
            if(null!=anterior.getString("issueSateliteLocationImg")){
                parametros.put("issueSateliteLocationImg", anterior.getString("issueSateliteLocationImg"));
            }
        }
    }
    
}
