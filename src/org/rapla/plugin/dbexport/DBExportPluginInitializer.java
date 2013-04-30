package org.rapla.plugin.dbexport;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.rapla.RaplaMainContainer;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
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
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.facade.ModificationModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.rapla.plugin.tableview.internal.AppointmentTableViewFactory;
import org.rapla.plugin.tableview.internal.ReservationTableViewFactory;

public class DBExportPluginInitializer extends RaplaGUIComponent implements IdentifiableMenuEntry,ActionListener
{
	
	String QUOTEBEFORE = "";
	String QUOTEAFTER = "";
	
	SimpleDateFormat sdfdate = null;
	SimpleDateFormat sdfdatetime = null;
	Locale locale = getRaplaLocale().getLocale();
	String SEPARATOR = "";
	String decimalpoint = "";
	String dateformat = "";
	String datetimeformat = "";
	String removeChars = "";
	boolean webstartEnabled = false;
	Category superCategory = getQuery().getSuperCategory();
	ClassificationFilter[] exportCriteria;
	CalendarModel model = getContext().lookup(CalendarModel.class);
	Reservation reservation;
	// temp fixing
	String srcTable;
	/*
	Category indicatieCatPatient;
	Category indicatorCatPatient;
	Category verwijzingCatPatient;
	Category bestemmingCatPatient;
	*/
	ModificationModule modificationMod = getClientFacade();
	Date startDate;
	Date endDate;

	String id = getString("database");
	JMenuItem item;
	
	public DBExportPluginInitializer(RaplaContext sm) throws RaplaException {
        super(sm);
        setChildBundleName( DBExportPlugin.RESOURCE_FILE);
        webstartEnabled =getContext().lookup(StartupEnvironment.class).getStartupMode() == StartupEnvironment.WEBSTART;
    
		JMenuItem item = new JMenuItem( id);
        item.setIcon( getIcon("icon.export") );
        item.addActionListener(this);
        
	}

	public JMenuItem getMenuElement() {
		return item;
	}

	public String getId() {
		return id;
	}
    public void actionPerformed(ActionEvent evt) {
        try {
    		if(isSelectionOK())
    			export( model);	
        } catch (Exception ex) {
            showException( ex, getMainComponent() );
        }
    }
    
    public void export(final CalendarModel model) throws IOException,  RaplaException
    {
    	SEPARATOR = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.separator_CONFIG,DBExportOption.SEPARATOR_SEMICOLON);
    	decimalpoint = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.decimalpoint_CONFIG,DBExportOption.DECIMAL_POINT);
    	dateformat = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.dateformat_CONFIG,DBExportOption.FORMAT_YYYYMMDD);
    	datetimeformat = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.datetimeformat_CONFIG,DBExportOption.FORMAT_YYYYMMDDHHMMSS);
    	sdfdate = new SimpleDateFormat(dateformat,locale);
    	sdfdate.setTimeZone(DateTools.getTimeZone());
    	sdfdatetime = new SimpleDateFormat(datetimeformat,locale);
    	sdfdatetime.setTimeZone(DateTools.getTimeZone());
    	QUOTEAFTER = QUOTEBEFORE = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.quote_CONFIG,DBExportOption.QUOTE_NONE);
    	removeChars = getQuery().getPreferences(getUser()).getEntryAsString( DBExportOption.removeChars_CONFIG,SEPARATOR);

    	// Pass request for treatment
/*
	    Set reservationTypes = model.getSelectedTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
	    if ( reservationTypes.size() != 1 ) {
    		DialogUI dialog2 = DialogUI.create(
                     							 getContext()
							                    ,getMainComponent()
							                    ,true
							                    ,getString("error")
							                    ,getString("no_eventtype")
							                    ,new String[] {getString("back")}
    										  );
            dialog2.setIcon(getIcon("icon.error"));
            dialog2.setDefault(0);
            dialog2.start();
    		return;
    	}    
*/
    	List<AppointmentBlock> blocks = getBlocksForRequest(model); 
    	if(blocks.size()==0) {
    		DialogUI dialog2 = DialogUI.create(
                    getContext()
                    ,getMainComponent()
                    ,true
                    ,getString("error")
                    ,getString("no_data")
                    ,new String[] {getString("back")}
           );
            dialog2.setIcon(getIcon("icon.error"));
            dialog2.setDefault(0);
            dialog2.start();
    		return;
    	}
    	StringBuffer out = new StringBuffer();
    	
    	// export Meta data
    	{
	    	// Object Id
	    	out.append(getColumnName (getString("Id")));       	
	        // Observation start date
	        out.append(SEPARATOR + getColumnName (getString("Date"))); 
	    	
	        Appointment app = blocks.get( 0 ).getAppointment();
	        Reservation reservation = app.getReservation();
	        
	        Allocatable[] persons = reservation.getPersons();
	        allocatableMetaData(persons, true, out, app); 
	        
	        Allocatable[] resources = reservation.getResources();
	        allocatableMetaData(resources, false, out, app);
	        
	        reservationMetaData(app, out); 
	        
	    	out.append("\n");  
    	}
    	
    	// export data 
	    for ( int i=0;i< blocks.size();i++)
	    {
	    	// Object Id
	    	out.append((i+1));   
	    	
	        // Observation start date
	        out.append(SEPARATOR + sdfdate.format( new Date(blocks.get(i).getStart())) ); 
	        // Observation start time
	        //out.append(SPACE + sdfHHmmss.format( new Date(blocks.getStartAt(i))) + QUOTEAFTER); 	
	        // Observation end date
	        //out.append(SEPARATOR + QUOTE + sdfyyyyMMdd.format( new Date(blocks.getEndAt(i)) ));		
	        // Observation end time
	        //out.append(SPACE + sdfHHmmss.format( new Date(blocks.getEndAt(i))) + QUOTEAFTER);	

	        Appointment app = blocks.get( i ).getAppointment();
	        	                
	        reservation = app.getReservation();
	        Allocatable[] persons = reservation.getPersons();
	        allocatableData(persons, true, out, app); 
	        
	        Allocatable[] resources = reservation.getResources();
	        allocatableData(resources, false, out, app);
	        
	        reservationData(app, out, blocks.get(i).getStart() ); 
	        
	    	out.append("\n");  
	    }
		DateFormat sdfyyyyMMdd = new SimpleDateFormat("yyyyMMdd");
		final String calendarName = getQuery().getPreferences( null ).getEntryAsString(RaplaMainContainer.TITLE, getString("rapla.title"));
		if(DateTools.countDays(startDate, endDate) == 1)
			saveFile( 
					 out.toString().getBytes()
					,calendarName
					 + "-"
					 + sdfyyyyMMdd.format( startDate )
					 + ".csv"
					 ,"csv"
					 ,getMainComponent()
					);
		else
			saveFile(
					 out.toString().getBytes()
					,calendarName 
					 + "-"
					 + sdfyyyyMMdd.format( startDate )
					 + "-"
					 + sdfyyyyMMdd.format( endDate )
					 + ".csv"
					,"csv"
					,getMainComponent()
					);
    }
    
	public String getColumnName (String name) {
    	return (QUOTEBEFORE + name.replace('_',' ') + QUOTEAFTER);
	}

	public void saveFile(byte[] content,String filename, String extension, final Component parentComponent) throws RaplaException {
		final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
		IOInterface io =  getService( IOInterface.class);
		String path = null;
		try {
			path = io.saveFile( frame, null, new String[] {extension}, filename, content);
    	} catch (IOException e) {
    		//throw new RaplaException("Can not export file!", e);
    		confirmSaveError(e.getMessage(), parentComponent);
    	}
    	
    	if(path != null && !webstartEnabled) {
			try {
				File xls = new File(path);
				Desktop desktop = null;
			    if (Desktop.isDesktopSupported()) {
			    	desktop = Desktop.getDesktop();
			    	if (desktop != null)
			    		desktop.open(xls);
			    }
			} catch (IOException ioe) {
			      ioe.printStackTrace();
			}
    	}
	}
	
	
	 private void confirmSaveError(String msg, final Component parentComponent) throws RaplaException {
		 DialogUI dialog2 = DialogUI.create(
                 getContext()
                 ,parentComponent
                 ,true
                 ,getString("error")
                 ,msg
                 ,new String[] {getString("back")}
        );
         dialog2.setIcon(getIcon("icon.error"));
         dialog2.setDefault(0);
         dialog2.start();
         return;
	    }
     
	
	private List<AppointmentBlock> getBlocksForRequest(CalendarModel model) throws  RaplaException {

		startDate = model.getStartDate();
		endDate = model.getEndDate();

		//Allocatable[] allocatables = getQuery().getAllocatables();
    
		// get events resspecting http arguments and resulting filtered resources
		Reservation[] events = getQuery().getReservations((User)null, startDate, endDate, exportCriteria);
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

	private void reservationMetaData(Appointment app, StringBuffer out) {
		Reservation reservation = app.getReservation();
        // Reservation Information
        Classifiable classifiable = (Classifiable) reservation;
        Classification classification = classifiable.getClassification();
        String tableName = classification.getType().getName(locale);
        
        //out.append(SEPARATOR + getColumnName (tableName + "_" + getString("Type_Id")));					

        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("eventname")));

        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("Status")));													
        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("Confirmation")));													
        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("Id")));													
        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("Start_Date")));
        out.append(SEPARATOR + getColumnName (tableName + "_" + getString("End_Date")));

		attributesMetaData(tableName, classification,out);
		
		out.append(SEPARATOR + getColumnName (getString("createdby")) );
		out.append(SEPARATOR + getColumnName (getString("createdat")) ); 
		out.append(SEPARATOR + getColumnName (getString("changedby")) );
		out.append(SEPARATOR + getColumnName (getString("changedat")) );								  
	}
	
	private void reservationData(Appointment app, StringBuffer out, long startDate) {
		Reservation reservation = app.getReservation();
        Classifiable classifiable = (Classifiable) app.getReservation();
        Classification classification = classifiable.getClassification();
        
        // Reservation_Type_Id PK
		//String ResKey = classification.getType().getElementKey();
        //out.append(SEPARATOR + QUOTEBEFORE + ResKey + QUOTEAFTER );													

        out.append(SEPARATOR + QUOTEBEFORE + classification.getType().getName(locale) + QUOTEAFTER);

        if(app.getRepeating() != null) // null for 1x
        	if(app.getRepeating().isException(startDate))
        		out.append(SEPARATOR + QUOTEBEFORE + getString("Cancelled") + QUOTEAFTER);
        	else
        		out.append(SEPARATOR + QUOTEBEFORE + getString("Executed") + QUOTEAFTER);
        else
        	out.append(SEPARATOR + QUOTEBEFORE + getString("Executed") + QUOTEAFTER);
/*
// BJO 00000093        
        if(app.isConfirmed())
       		out.append(SEPARATOR + QUOTEBEFORE + getString("Confirmed") + QUOTEAFTER);
       	else
       		out.append(SEPARATOR + QUOTEBEFORE + getString("Notconfirmed") + QUOTEAFTER);
// BJO 00000093
*/
        // Reservation_ID
        String reservationdId = RaplaDBExport.getShortId((RefEntity)classifiable);
        out.append(SEPARATOR + reservationdId);	
        
        // Reservation_Start_Date
        out.append(SEPARATOR + findStartDate(app));	
        
        // Reservation_End_Date
        out.append(SEPARATOR + findEndDate(app));
        
        // Reservation Attributes
		srcTable = classification.getType().getName(locale);
		attributesData(classification,out);		
		
		out.append(SEPARATOR + QUOTEBEFORE + reservation.getOwner().getName() + QUOTEAFTER);
		out.append(SEPARATOR + QUOTEBEFORE + sdfdatetime.format( reservation.getCreateTime() ) + QUOTEAFTER);
		if(reservation.getLastChangedBy() == null)
			out.append(SEPARATOR + QUOTEBEFORE + reservation.getOwner().getName() + QUOTEAFTER);
		else
			out.append(SEPARATOR + QUOTEBEFORE + reservation.getLastChangedBy().getName() + QUOTEAFTER);
		out.append(SEPARATOR + QUOTEBEFORE + sdfdatetime.format( reservation.getLastChangeTime() ) + QUOTEAFTER);

        // Reservation type name English
        //Locale locale = new Locale("EN");
        //out.append(SEPARATOR + QUOTE + classification.getType().getName(locale) + QUOTEAFTER); 
        // Reservation type name
        // Reservation display name ( not required )
        //out.append(SEPARATOR + reservation.getName(locale));									  
	}
 
    private String findStartDate(Appointment app) {
        //Calendar cal= DateTools.createGMTCalendar();   
        //cal.setTime(app.getStart());
        return sdfdatetime.format(app.getStart());
    }

    private String findEndDate(Appointment app) {
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
	
	private void allocatableMetaData(Allocatable[] alls, boolean isPerson, StringBuffer out, Appointment appointment) {
		if (alls.length > 0) {
		    //out.append(SEPARATOR + getString("Type"));
		    for (int j=0;j<alls.length;j++) {	
				boolean hasAlloc=appointment.getReservation().hasAllocated(alls[j],appointment);
				if(hasAlloc)
				{
					Classification classification = alls[j].getClassification();
					String tableName = classification.getType().getName(locale);
					
					//out.append(SEPARATOR + getColumnName (tableName + "_" +  getString("Type_Id"))); 
					//out.append(SEPARATOR + getColumnName (tableName + "_" +  getString("Type_Name")));
					
					out.append(SEPARATOR + getColumnName (tableName + "_" +  getString("Id")));
					// out.append(SEPARATOR + getColumnName (tableName + "_" +  getString("origin")));
					attributesMetaData(tableName, classification,out);
					
				}
		    }
		}
	}

	private void allocatableData(Allocatable[] alls, boolean isPerson, StringBuffer out, Appointment appointment) {
		// loop through an array of allocatables, fetches every attributes, and outputs to DB
		//String blockName = (isPerson)? "P" : "R";
		if (alls.length > 0) {
			// Resource_Type
		    //out.append(SEPARATOR + QUOTEBEFORE + blockName + QUOTEAFTER);
		    
		    for (int j=0;j<alls.length;j++) {	
				boolean hasAlloc=appointment.getReservation().hasAllocated(alls[j],appointment);
				if(hasAlloc)
				{
					Classification classification = alls[j].getClassification();
					
					// Resource_Type_Id
					//String allKey = classification.getType().getElementKey();
					//out.append(SEPARATOR + QUOTEBEFORE + allKey + QUOTEAFTER); 
					
					// Resource_Type_Name
					//out.append(SEPARATOR + QUOTEBEFORE + classification.getType().getName(locale) + QUOTEAFTER);
					
					// Resource_Id
					String allocatableId = RaplaDBExport.getShortId( (RefEntity) alls[j]);

					out.append(SEPARATOR + allocatableId);			
				}
		    }
		}
	}

	private void attributesData(Classification classification, StringBuffer out) {
	
		Attribute[] attributes = classification.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			String attributeKey = attributes[k].getKey();
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
				Category cat = (Category) classification.getValue(attributeKey);  
				if ( cat != null ) {
// BJO 00000091 
		            Category rootCategory = (Category) attributes[k].getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
		            out.append(SEPARATOR + QUOTEBEFORE + cat.getPath(rootCategory, locale).replaceAll(removeChars, " ") + QUOTEAFTER);
// BJO 00000091
					//out.append(SEPARATOR + QUOTEBEFORE + cat.getPath(superCategory, locale).replace("/", " ") + QUOTEAFTER);
		        }
				else {
					out.append(SEPARATOR + QUOTEBEFORE + QUOTEAFTER);
				}
	            /* temp FIX
				try {
	            if(fix && srcTable.equals("Patient")) {
	            	if(attributeKey.equals("indicatie")) {
	            		indicatieCatPatient = cat;
	            	}
	            	else
	            		if(attributeKey.equals("indicator")) {
		            		indicatorCatPatient = cat;
		            	}	
		            	else
		            		if(attributeKey.equals("verwijzing")) {
			            		verwijzingCatPatient = cat;
			            	}	
			            	else
			            		if(attributeKey.equals("bestemming")) {
				            		bestemmingCatPatient = cat;
				            	}	
	            }
	            if(fix & srcTable.equals("Opname")) {
	            	if(attributeKey.equals("indicatie")) {
	    	    		Reservation persistantReservation = (Reservation) modificationMod.getPersistant( reservation );
	    	            Reservation clone = (Reservation) modificationMod.edit(persistantReservation);
	            		clone.getClassification().setValue("indicatie", indicatieCatPatient);
	                    modificationMod.storeObjects(new Entity[] {clone});
	            	}
	            	else
	            		if(attributeKey.equals("indicator")) {
		    	    		Reservation persistantReservation = (Reservation) modificationMod.getPersistant( reservation );
		    	            Reservation clone = (Reservation) modificationMod.edit(persistantReservation);
		    	            clone.getClassification().setValue("indicator", indicatorCatPatient);
		                    modificationMod.storeObjects(new Entity[] {clone});
		            	}	
		            	else
		            		if(attributeKey.equals("verwijzing")) {
			    	    		Reservation persistantReservation = (Reservation) modificationMod.getPersistant( reservation );
			    	            Reservation clone = (Reservation) modificationMod.edit(persistantReservation);
			    	            clone.getClassification().setValue("verwijzing", verwijzingCatPatient);
			                    modificationMod.storeObjects(new Entity[] {clone});
			            	}	
			            	else
			            		if(attributeKey.equals("bestemming")) {
				    	    		Reservation persistantReservation = (Reservation) modificationMod.getPersistant( reservation );
				    	            Reservation clone = (Reservation) modificationMod.edit(persistantReservation);
				    	            clone.getClassification().setValue("bestemming", bestemmingCatPatient);
				                    modificationMod.storeObjects(new Entity[] {clone});
				            	}	
	            }
				} catch (RaplaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            // temp FIX */
			}
			else 
				if (attributes[k].getType() == AttributeType.DATE) {
					Object attributeValue = classification.getValue(attributeKey);
					if ( attributeValue != null) {
						out.append(SEPARATOR + sdfdate.format( attributeValue ));
					}
					else {
						out.append(SEPARATOR);
					}                		
				}						
				else 
					if (attributes[k].getType() == AttributeType.STRING) {
						Object attributeValue = classification.getValue(attributeKey);
						if ( attributeValue != null) {
							out.append(SEPARATOR + QUOTEBEFORE + attributeValue.toString().replaceAll(removeChars, " ") + QUOTEAFTER);
						}
						else {
							out.append(SEPARATOR + QUOTEBEFORE + QUOTEAFTER);
						}                		
					}
					else 
						if (attributes[k].getType() == AttributeType.INT) {
							Object attributeValue = classification.getValue(attributeKey);
							if ( attributeValue != null) {
								out.append(SEPARATOR + attributeValue.toString());
							}
							else {
								out.append(SEPARATOR);
							}                		
						}
						else 
							if (attributes[k].getType() == AttributeType.BOOLEAN) {
								Object attributeValue = classification.getValue(attributeKey);
								if ( attributeValue != null) {
									out.append(SEPARATOR + QUOTEBEFORE + getString(attributeValue.toString()) + QUOTEAFTER);
								}
								else {
									out.append(SEPARATOR);
								}                		
							}
		}
	}
	
	private void attributesMetaData(String tableName, Classification classification, StringBuffer out) {
		
		Attribute[] attributes = classification.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			//String attributeKey = attributes[k].getKey();
			String attributeName = attributes[k].getName(locale);
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
					out.append(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
			}
			else 
				if (attributes[k].getType() == AttributeType.DATE) {
					out.append(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
				}						
				else 
					if (attributes[k].getType() == AttributeType.STRING) {
						out.append(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
					}
					else 
						if (attributes[k].getType() == AttributeType.INT) {
							out.append(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
						}
						else 
							if (attributes[k].getType() == AttributeType.BOOLEAN) {
								out.append(SEPARATOR + getColumnName (tableName + "_" + attributeName) );
							}
		}
	}

	private boolean isSelectionOK() throws RaplaException {
		exportCriteria = ((CalendarSelectionModel) model).getReservationFilter();
	    boolean selectionOK = false;
	    
    	if(   
    		  ((CalendarSelectionModel)model).getViewId( ).equals(ReservationTableViewFactory.TABLE_VIEW) 
    	   || ((CalendarSelectionModel)model).getViewId( ).equals(AppointmentTableViewFactory.TABLE_VIEW) ) {
    		selectionOK = true;
    	}
    	
    	if(!selectionOK) {
    		DialogUI dialog2 = DialogUI.create(
	                 							 getContext()
							                    ,getMainComponent()
							                    ,true
							                    ,getString("error")
							                    ,getString("no_selection")
							                    ,new String[] {getString("back")}
											  );
	        dialog2.setIcon(getIcon("icon.error"));
	        dialog2.setDefault(0);
	        dialog2.start();
		}    
		return selectionOK;
	}
}