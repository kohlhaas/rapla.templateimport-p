/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Bob Jordaens                                          |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.dbexport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.rapla.RaplaMainContainer;
import org.rapla.components.util.DateTools;
import org.rapla.components.xmlbundle.LocaleSelector;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.components.xmlbundle.impl.LocaleSelectorImpl;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.storage.RefEntity;
import org.rapla.examples.SimpleConnectorStartupEnvironment;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.Configuration;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaDefaultContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.framework.logger.ConsoleLogger;
import org.rapla.framework.logger.Logger;
import org.rapla.gui.internal.RaplaStartOption;


public class RaplaDBExport  {
	
	static String QUOTEBEFORE = "";
	static String QUOTEAFTER = "";
	static SimpleDateFormat sdfdate = null;
	static SimpleDateFormat sdfdatetime = null;
	static String SEPARATOR = "";
	static String decimalpoint = "";
	static String dateformat = "";
	static String datetimeformat = "";
	static String removeChars = "";
	boolean webstartEnabled = false;
	static Reservation reservation;	
	static ClientFacade facade ;
	static Logger logger = new ConsoleLogger( ConsoleLogger.LEVEL_INFO);
	static RaplaMainContainer container;
    static I18nBundleImpl i18n;
    static LocaleSelector localeSelector;
	static Locale locale;
	static String eventType= "";
	static String workDir = "";

    public static void main(String[] args) {
    	int exit = 0;
        if ( args.length < 5 ) {
            System.out.println("Usage: User Password Host Hostport eventname UNC");
            return;
        }
        
        try
        {
        	setUp();
            StartupEnvironment env = new SimpleConnectorStartupEnvironment(  args[2], Integer.valueOf(args[3]), "/",false, logger);
            logger.info("URL: " + env.getDownloadURL());
            container = new RaplaMainContainer( env);
            facade = (ClientFacade)container.getContext().lookup(ClientFacade.ROLE);
            
            if ( facade.login(args[0], args[1].toCharArray() ) ) {
            	ClassificationFilter[] exportCriteria = new ClassificationFilter[1];
            	eventType = args[4];
            	if(args.length == 6)
            		workDir = args[5];
            	DynamicType[] types = facade.getDynamicTypes(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
            	DynamicType oneType = null;
            	for (DynamicType type: types)
            	{
            	    if ( type.getName( locale).equals( eventType))
            	    {
            	        oneType = type;
            	    }
            	}
            	if ( oneType == null)
            	{
            	      throw new EntityNotFoundException("No dynamictype with name " + oneType);
            	}
            	     
            	ClassificationFilter oneFilter =  oneType.newClassificationFilter();
            	exportCriteria[0] = (ClassificationFilter) oneFilter;
            	int count = export( facade.today(),DateTools.fillDate(facade.today()),exportCriteria);
            	logger.info("Records produced: " + count);
            	facade.logout();
            }
          	else {
          		logger.info("Logon failed: " + args[0]);
            	System.exit(1);
          	}
        }
        catch ( Exception e ) 
        {
            logger.error(e.getMessage());
            exit = 1;
        }
        container.dispose();
        System.exit(exit);
    }
    
	public static int export(Date startDate, Date endDate, ClassificationFilter[] filter) throws IOException, RaplaException
    {
    	SEPARATOR = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.separator_CONFIG,DBExportOption.SEPARATOR_SEMICOLON);
    	decimalpoint = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.decimalpoint_CONFIG,DBExportOption.DECIMAL_POINT);
    	dateformat = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.dateformat_CONFIG,DBExportOption.FORMAT_YYYYMMDD);
    	datetimeformat = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.datetimeformat_CONFIG,DBExportOption.FORMAT_YYYYMMDDHHMMSS);
    	sdfdate = new SimpleDateFormat(dateformat,locale);
    	sdfdate.setTimeZone(DateTools.getTimeZone());
    	sdfdatetime = new SimpleDateFormat(datetimeformat,locale);
    	sdfdatetime.setTimeZone(DateTools.getTimeZone());
    	QUOTEAFTER = QUOTEBEFORE = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.quote_CONFIG,DBExportOption.QUOTE_NONE);
    	removeChars = facade.getPreferences(facade.getUser()).getEntryAsString( DBExportOption.removeChars_CONFIG,SEPARATOR);

    	List<AppointmentBlock> blocks = getBlocksForRequest(startDate, endDate, filter); 
    	if(blocks.size()==0) {
            logger.error(i18n.getString("no_data"));
    		return 0;
    	}
    	
    	PrintStream out = openOutput(eventType,startDate,endDate);
    	
    	// export Meta data
    	{
	    	// Object Id
	    	out.print(getColumnName (i18n.getString("Id")));       	
	        // Observation start date
	        out.print(SEPARATOR + getColumnName (i18n.getString("Date"))); 
	    	
	        Appointment app = blocks.get( 0 ).getAppointment();
	        Reservation reservation = app.getReservation();
	        
	        Allocatable[] persons = reservation.getPersons();
	        allocatableMetaData(persons, true, out, app); 
	        
	        Allocatable[] resources = reservation.getResources();
	        allocatableMetaData(resources, false, out, app);
	        
	        reservationMetaData(app, out); 
	        
	    	out.print("\n");  
    	}
    	
    	// export data
    	int i = 0;
	    for (;i< blocks.size();i++)
	    {
	    	// Object Id
	    	out.print((i+1));   
	    	
	        // Observation start date
	        out.print(SEPARATOR + sdfdate.format( new Date(blocks.get(i).getStart())) ); 
	        // Observation start time
	        //out.print(SPACE + sdfHHmmss.format( new Date(blocks.getStartAt(i))) + QUOTEAFTER); 	
	        // Observation end date
	        //out.print(SEPARATOR + QUOTE + sdfyyyyMMdd.format( new Date(blocks.getEndAt(i)) ));		
	        // Observation end time
	        //out.print(SPACE + sdfHHmmss.format( new Date(blocks.getEndAt(i))) + QUOTEAFTER);	

	        Appointment app = blocks.get( i ).getAppointment();
	        	                
	        reservation = app.getReservation();
	        Allocatable[] persons = reservation.getPersons();
	        allocatableData(persons, true, out, app); 
	        
	        Allocatable[] resources = reservation.getResources();
	        allocatableData(resources, false, out, app);
	        
	        reservationData(app, out, blocks.get(i).getStart() ); 
	        
	    	out.print("\n");  
	    }
    	return i;
    }
    
	public static String getColumnName (String name) {
    	return (QUOTEBEFORE + name.replace('_',' ') + QUOTEAFTER);
	}

	private static List<AppointmentBlock> getBlocksForRequest(Date startDate, Date endDate, ClassificationFilter[] filter) throws RaplaException {

		//Allocatable[] allocatables = getQuery().getAllocatables();
    
		// get events respecting http arguments and resulting filtered resources
		Reservation[] events = facade.getReservations((User)null, startDate, endDate, filter);  
		
		//Reservation[] events = ((CalendarSelectionModel) model).getReservations(startDate, endDate);
		// build the blocks
		List<AppointmentBlock> blocks = new ArrayList();
	
		for ( int i=0;i<events.length;i++)
		{
			Reservation event = events[i];
			Appointment[] appointments = event.getAppointments();
			for ( int j=0;j< appointments.length;j++)
			{
				Appointment appointment = appointments[j];
				appointment.createBlocks ( startDate, endDate, blocks, false); // with exceptions (= deleted appointments) !!!!
			}
		}
		// sort them by time
		//blocks.sort();
		return blocks;
    
	}

	private static void reservationMetaData(Appointment app, PrintStream out) {
		Reservation reservation = app.getReservation();
        // Reservation Information
        Classifiable classifiable = (Classifiable) reservation;
        Classification classification = classifiable.getClassification();
        String tableName = classification.getType().getName(locale);
        
        //out.print(SEPARATOR + getColumnName (tableName + "_" + getString("Type_Id")));					

        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("reservation.name")));

        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("Status")));													
        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("Confirmation")));													
        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("Id")));													
        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("Start_Date")));
        out.print(SEPARATOR + getColumnName (tableName + "_" + i18n.getString("End_Date")));

		attributesMetaData(tableName, classification,out);												
								  
	}
	
	private static void reservationData(Appointment app, PrintStream out, long startDate) {
		// Reservation reservation = app.getReservation();
        Classifiable classifiable = (Classifiable) app.getReservation();
        Classification classification = classifiable.getClassification();
        
        // Reservation_Type_Id PK
		//String ResKey = classification.getType().getElementKey();
        //out.print(SEPARATOR + QUOTEBEFORE + ResKey + QUOTEAFTER );													

        out.print(SEPARATOR + QUOTEBEFORE + classification.getType().getName() + QUOTEAFTER);

        if(app.getRepeating() != null) // null for 1x
        	if(app.getRepeating().isException(startDate))
        		out.print(SEPARATOR + QUOTEBEFORE + i18n.getString("Cancelled") + QUOTEAFTER);
        	else
        		out.print(SEPARATOR + QUOTEBEFORE + i18n.getString("Executed") + QUOTEAFTER);
        else
        	out.print(SEPARATOR + QUOTEBEFORE + i18n.getString("Executed") + QUOTEAFTER);
/*
// BJO 00000093        
        if(app.isConfirmed())
       		out.print(SEPARATOR + QUOTEBEFORE + i18n.getString("Confirmed") + QUOTEAFTER);
       	else
       		out.print(SEPARATOR + QUOTEBEFORE + i18n.getString("Notconfirmed") + QUOTEAFTER);
// BJO 00000093
 */

        // Reservation_ID
        String reservationdId = getShortId(((RefEntity)classifiable));
        out.print(SEPARATOR + reservationdId);	
        
        // Reservation_Start_Date
        out.print(SEPARATOR + findStartDate(app));	
        
        // Reservation_End_Date
        out.print(SEPARATOR + findEndDate(app));
        
        // Reservation Attributes
		attributesData(classification,out);												

        // Reservation type name English
        //Locale locale = new Locale("EN");
        //out.print(SEPARATOR + QUOTE + classification.getType().getName(locale) + QUOTEAFTER); 
        // Reservation type name
        // Reservation display name ( not required )
        //out.print(SEPARATOR + reservation.getName(locale));									  
	}
 
	public static String getShortId(RefEntity<?> refEntity) {
	    Object id = refEntity.getId();
	    if ( id != null)
	    {
	        return id.toString().split("_")[1];
	    }
	    return null;
    }

    private static String findStartDate(Appointment app) {
        //Calendar cal= DateTools.createGMTCalendar();   
        //cal.setTime(app.getStart());
        return sdfdatetime.format(app.getStart());
    }

    private static String findEndDate(Appointment app) {
    	Repeating r = app.getRepeating();
    	Date edate = null;
    	if ( r == null )
    		edate = app.getEnd();
    	else 
    		if ( r.getEnd() != null && !r.isFixedNumber() ) {
    			edate =  DateTools.subDay(r.getEnd());
    		}
    		else {
    			if (r.getEnd() != null)
    				edate = r.getEnd();
    			else {// set date to 9999-12-31
    				Calendar cal= DateTools.createGMTCalendar();   
    				cal.set(Calendar.YEAR, 9999); 
    				cal.set(Calendar.MONTH, Calendar.DECEMBER); 
    				cal.set(Calendar.DAY_OF_MONTH, 31); 
    				cal.set(Calendar.HOUR_OF_DAY, 0); 
    				cal.set(Calendar.MINUTE, 0);  
    				cal.set(Calendar.SECOND, 0);
    				cal.set(Calendar.MILLISECOND, 0);
    				edate = cal.getTime();
    				}
    		}
        //Calendar cal= DateTools.createGMTCalendar();   
        //cal.setTime(edate);
        return sdfdatetime.format(edate);
    }
	
	private static void allocatableMetaData(Allocatable[] alls, boolean isPerson, PrintStream out, Appointment appointment) {
		if (alls.length > 0) {
		    //out.print(SEPARATOR + getString("Type"));
		    for (int j=0;j<alls.length;j++) {	
				boolean hasAlloc=appointment.getReservation().hasAllocated(alls[j],appointment);
				if(hasAlloc)
				{
					Classification classification = alls[j].getClassification();
					String tableName = classification.getType().getName(locale);
					
					//out.print(SEPARATOR + getColumnName (tableName + "_" +  getString("Type_Id"))); 
					//out.print(SEPARATOR + getColumnName (tableName + "_" +  getString("Type_Name")));
					
					out.print(SEPARATOR + getColumnName (tableName + "_" +  i18n.getString("Id")));
					//out.print(SEPARATOR + getColumnName (tableName + "_" +  i18n.getString("Origin")));
					attributesMetaData(tableName, classification,out);				
				}
		    }
		}
	}

	private static void allocatableData(Allocatable[] alls, boolean isPerson, PrintStream out, Appointment appointment) {
		// loop through an array of allocatables, fetches every attributes, and outputs to DB
		if (alls.length > 0) {
		    
		    for (int j=0;j<alls.length;j++) {	
				boolean hasAlloc=appointment.getReservation().hasAllocated(alls[j],appointment);
				if(hasAlloc)
				{
					//Classification classification = alls[j].getClassification();
					
					// Resource_Type_Id
					//String allKey = classification.getType().getElementKey();
					//out.print(SEPARATOR + QUOTEBEFORE + allKey + QUOTEAFTER); 
					
					// Resource_Type_Name
					//out.print(SEPARATOR + QUOTEBEFORE + classification.getType().getName(locale) + QUOTEAFTER);
					
					// Resource_Id
					String allocatableId = getShortId(((RefEntity) alls[j]));

					out.print(SEPARATOR + allocatableId);			
				}
		    }
		}
	}

	private static void attributesData(Classification classification, PrintStream out) {
	
		Attribute[] attributes = classification.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			String attributeKey = attributes[k].getKey();
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
				Category cat = (Category) classification.getValue(attributeKey);  
				if ( cat != null ) {
// BJO 00000091 
		            Category rootCategory = (Category) attributes[k].getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
		            out.print(SEPARATOR + QUOTEBEFORE + cat.getPath(rootCategory, locale).replaceAll(removeChars, " ") + QUOTEAFTER);
// BJO 00000091
					//out.print(SEPARATOR + QUOTEBEFORE + cat.getPath(superCategory, locale).replace("/", " ") + QUOTEAFTER);
		        }
				else {
					out.print(SEPARATOR + QUOTEBEFORE + QUOTEAFTER);
				}
			}
			else 
				if (attributes[k].getType() == AttributeType.DATE) {
					Object attributeValue = classification.getValue(attributeKey);
					if ( attributeValue != null) {
						out.print(SEPARATOR + sdfdate.format( attributeValue ));
					}
					else {
						out.print(SEPARATOR);
					}                		
				}						
				else 
					if (attributes[k].getType() == AttributeType.STRING) {
						Object attributeValue = classification.getValue(attributeKey);
						if ( attributeValue != null) {
							out.print(SEPARATOR + QUOTEBEFORE + attributeValue.toString().replaceAll(removeChars, " ") + QUOTEAFTER);
						}
						else {
							out.print(SEPARATOR + QUOTEBEFORE + QUOTEAFTER);
						}                		
					}
					else 
						if (attributes[k].getType() == AttributeType.INT) {
							Object attributeValue = classification.getValue(attributeKey);
							if ( attributeValue != null) {
								out.print(SEPARATOR + attributeValue.toString());
							}
							else {
								out.print(SEPARATOR);
							}                		
						}
						else 
							if (attributes[k].getType() == AttributeType.BOOLEAN) {
								Object attributeValue = classification.getValue(attributeKey);
								if ( attributeValue != null) {
									out.print(SEPARATOR + QUOTEBEFORE + attributeValue.toString() + QUOTEAFTER);
								}
								else {
									out.print(SEPARATOR);
								}                		
							}
		}
	}
	
	private static void attributesMetaData(String tableName, Classification classification, PrintStream out) {
		
		Attribute[] attributes = classification.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			//String attributeKey = attributes[k].getKey();
			String attributeName = attributes[k].getName(locale);
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
					out.print(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
			}
			else 
				if (attributes[k].getType() == AttributeType.DATE) {
					out.print(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
				}						
				else 
					if (attributes[k].getType() == AttributeType.STRING) {
						out.print(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
					}
					else 
						if (attributes[k].getType() == AttributeType.INT) {
							out.print(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
						}
						else 
							if (attributes[k].getType() == AttributeType.BOOLEAN) {
								out.print(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
							}
		}
	}
    protected static void setUp() throws Exception {
        DefaultConfiguration config = new DefaultConfiguration("i18n");
        config.setAttribute("id","org.rapla.plugin.dbexport.DBExportResources");
        i18n = create(config);
    }

    private static I18nBundleImpl create(Configuration config) throws Exception {
        I18nBundleImpl i18n;
        RaplaDefaultContext context = new RaplaDefaultContext();
        LocaleSelector localeSelector = new LocaleSelectorImpl();
        locale = localeSelector.getLocale();
        context.put(LocaleSelector.ROLE,localeSelector);
        i18n = new I18nBundleImpl(context,config,logger);
        return i18n;
    }
    
	private static PrintStream openOutput(String tableName, Date startDate, Date endDate) {
		try
		{          			
			DateFormat sdfyyyyMMdd = new SimpleDateFormat("yyyyMMdd");
			if(workDir.isEmpty())
				workDir = System.getProperty("user.dir");
			final String calendarName = facade.getPreferences( null ).getEntryAsString(RaplaStartOption.TITLE, i18n.getString("rapla.title"));
            String fileName = workDir + System.getProperty("file.separator") + calendarName + "-" + sdfyyyyMMdd.format( startDate ) + ".csv";
			FileOutputStream fstream = new FileOutputStream(fileName);
            logger.info("Producing: " + fileName);			
			return new PrintStream(fstream);
		} 
	    catch (Exception e)
	    {
	    	logger.error("ERROR-" + tableName + ".txt-" + "File output error:" + e.getMessage());
	    	System.exit(1);
	    }
	    return null;
	}
}
