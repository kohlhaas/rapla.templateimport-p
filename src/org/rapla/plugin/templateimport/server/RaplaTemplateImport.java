package org.rapla.plugin.templateimport.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.templateimport.ParsedTemplateResult;
import org.rapla.plugin.templateimport.TemplateImport;
import org.rapla.server.RemoteMethodFactory;
import org.rapla.server.RemoteSession;
import org.rapla.server.TimeZoneConverter;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.dbsql.DBOperator;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;


public class RaplaTemplateImport extends RaplaComponent implements RemoteMethodFactory<TemplateImport> , TemplateImport{

    public RaplaTemplateImport(RaplaContext context) {
        super(context);
    }

    public ParsedTemplateResult importFromServer() throws RaplaException
    {
        TimeZoneConverter converter = getContext().lookup( TimeZoneConverter.class);
        DBOperator operator = (DBOperator) getClientFacade().getOperator();
        ParsedTemplateResult result = new ParsedTemplateResult();
        Connection connection = null;
        try 
        {
            connection = operator.createConnection();
            PreparedStatement prepareStatement;
            prepareStatement = connection.prepareStatement("select * from vw_seminarimporte;" );
            ResultSet set = prepareStatement.executeQuery();
            ResultSetMetaData metaData = set.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] header = new String[columnCount];
            for ( int i= 0;i<columnCount;i++)
            {
                String column = metaData.getColumnName( i+1);
                header[i] = column;
            }
            result.setHeader(Arrays.asList(header));
            final CellProcessor[] processors = new CellProcessor[header.length];
            for ( int i=0;i<processors.length;i++)
            {
                processors[i]= new Optional();
            }
            int count = 0;
            while (set.next())
            {
                Map<String,String> map = new LinkedHashMap<String, String>();
                boolean ignore = false;
                for ( int i= 0;i<columnCount;i++)
                {
                    String column =header[i] ;
                    Object object = set.getObject(column);
                    if ( object != null)
                    {
                        String string;
                        if (object instanceof Date)
                        {
                            SerializableDateTimeFormat formater = getRaplaLocale().getSerializableFormat();
                            Date date = converter.toRaplaTime( converter.getImportExportTimeZone(),((Date) object));
							string = formater.formatDate( date);
//                            if ( column.equals( TemplateImport.BEGIN_KEY))
//                            {
//                                //
//                            	if ( date.before( getQuery().today()))
//                            	{
//                            		ignore = true;
//                            		break;
//                            	}
//                            }
                        }
                        else
                        {
                            string = object.toString().trim();
                        }
                        map.put( column, string);
                    }
                }   
                if ( !ignore)
                {
                    result.addTemplate(map);
                }
                count++;
            }
            getLogger().debug("Found " + count + " Entries ");
            
        } catch (Exception e) {
            throw new RaplaException("Error importing from database", e);
        }
        finally
        {
            if ( connection != null)
            {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new RaplaException(e.getMessage(), e);
                }
            }     
        }
       
        return result;
    }

    public boolean hasDBConnection() throws RaplaException {
        StorageOperator operator2 = getClientFacade().getOperator();
        if ( operator2 instanceof DBOperator)
        {
            DBOperator operator = (DBOperator) operator2;
            Connection connection = null;
            try 
            {
                connection = operator.createConnection();
                PreparedStatement prepareStatement;
                try
                {
                    prepareStatement = connection.prepareStatement("select * from vw_seminarimporte;" );
                    ResultSet set = prepareStatement.executeQuery();
                    return set.next();
                } catch (Exception e) {
                    return false;
                }
            } 
            catch (Exception e) 
            {
                throw new RaplaException("Error importing from database", e);
            }
            finally
            {
                if ( connection != null)
                {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        throw new RaplaException(e.getMessage(), e);
                    }
                }     
            }
        }
        
        return false;
    }

    public TemplateImport createService(RemoteSession remoteSession) {
        return this;
    }

}
