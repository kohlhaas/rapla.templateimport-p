/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
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
package org.rapla.plugin.templateimport;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.rapla.components.iolayer.FileContent;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationStartComparator;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.plugin.RaplaExtensionPoints;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;


public class ImportTemplatePluginInitializer extends RaplaGUIComponent
{


	public ImportTemplatePluginInitializer(RaplaContext sm) throws RaplaException {
        super(sm);
        //MenuExtensionPoint helpMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.HELP_MENU_EXTENSION_POINT);
        //helpMenu.insert(createInfoMenu() );
        if(!getUser().isAdmin())
        	return;
        setChildBundleName( ImportTemplatePlugin.RESOURCE_FILE);
        MenuExtensionPoint importMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.IMPORT_MENU_EXTENSION_POINT);
        importMenu.insert( createImportMenu());

        //MenuExtensionPoint export = (MenuExtensionPoint) getService( RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT);
        //export.insert(createExportMenu() );
    }


    private JMenuItem createImportMenu( ) {
        JMenuItem item = new JMenuItem( "events into templates" );
        item.setIcon( getIcon("icon.import") );
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
                        IOInterface io =  getService( IOInterface.class);
                        FileContent file = io.openFile( frame, null, new String[] {".csv"});
                        if ( file != null) 
                        {
                            Reader reader = new InputStreamReader( file.getInputStream());
                            ICsvMapReader mapReader = null;
                            List<Entry> list=new ArrayList<Entry>();
                            try {
                                    mapReader = new CsvMapReader(reader, CsvPreference.STANDARD_PREFERENCE);
                                    
                                    // the header columns are used as the keys to the Map
                                    final String[] header = mapReader.getHeader(true);
                                    final CellProcessor[] processors = new CellProcessor[header.length];
                                    for ( int i=0;i<processors.length;i++)
                                    {
                                    	processors[i]= new Optional();
                                    }
                                    
                                    Map<String, Object> customerMap;
                                    while( (customerMap = mapReader.read(header, processors)) != null ) 
                                    {
                                    	 list.add(new Entry(customerMap));
                                    }
                                    confirmImport(frame, header, list);
                                    
                            }
                            finally {
                                    if( mapReader != null ) {
                                            mapReader.close();
                                    }
                            }
                            //String[][] entries = Tools.csvRead( reader, ',',1 ); // read first 5 colums per input row and store in memory
                        }
                     } catch (Exception ex) {
                        showException( ex, getMainComponent() );
                    }
                }
        });
        return item;
    }

    public class JIDCellEditor extends AbstractCellEditor implements TableCellEditor {

	    JComboBox jComboBox;
	    List<Template> templates = new ArrayList<ImportTemplatePluginInitializer.Template>();

	    JIDCellEditor( List<Template> templates)
	    {
	    	this.templates = templates;	
	    }
	    
	    public Object getCellEditorValue() {
	        return jComboBox.getSelectedItem();
	    }

	    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
	        Vector vector = new Vector();
	        vector.addAll( templates);
	        jComboBox = new JComboBox(vector);
	        jComboBox.setSelectedItem(value);
	        return jComboBox;
	    }
	}

    
    class Template
    {
    	public Reservation[] reservations;
    	public Template(Reservation[] reservations)
    	{
    		this.reservations = reservations;
    	}
    	
    	public String toString()
    	{
    		return reservations[0].getName( getLocale());
    	}
    };
    
    enum Status
    {
    	zu_loeschen,
    	aktuallisieren,
    	template,
    	template_waehlen,
    	geloescht, 
    	datum_fehlt, datum_fehlerhaft, aktuell
    }

    private static final String PRIMARY_KEY = "SeminarNr";
    private static final String TEMPLATE_KEY = "TitelID";
    
    class Entry
    {
    	Map<String, Object> entries;
		public Reservation[] reservations;
		Template template;
		
    	public Entry(Map<String, Object> entries) {
    		this.entries = entries;
    	}

    	
    	public Date getBeginn() throws ParseException
    	{
    		String dateString = (String)entries.get("DatumVon");
    		if (dateString != null)
    		{
    			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    			format.setTimeZone( DateTools.getTimeZone());
    			Date parse = format.parse( dateString);
				return parse;
    		}
    		return null;
    	}
    	
       	public Object get(String key)
       	{
       		return entries.get( key);
       	}
       	
       	public boolean isStorno()
       	{
       		String string = (String)entries.get("StorniertAm");
       		if ( string != null && string.trim().length() > 0)
       		{
       			return true;
       		}
       		return false;
       	}


		public Status getStatus() 
		{
			boolean hasReservations = reservations != null && reservations.length >0;
			boolean hasTemplates = template != null;
			try {
				if ( getBeginn() == null)
				{
					return Status.datum_fehlt;
				}
			} catch (ParseException e) {
				return Status.datum_fehlerhaft;
			}
			if ( isStorno())
			{
				if ( hasReservations)
				{
					return Status.zu_loeschen;
				}
				else
				{
					return Status.geloescht;
				}
			}
			if ( hasReservations)
			{
				if ( needsUpdate( reservations, entries))
				{
					return Status.aktuallisieren;
				}
				else
				{
					return Status.aktuell;
				}
			}
			if ( hasTemplates)
			{
				return Status.template;
			}
			else
			{
				return Status.template_waehlen;
			}
			
		}


		public void process() throws ParseException, RaplaException {
			Status status =getStatus();
			switch (status )
			{
				case geloescht: ;break;
				case aktuallisieren: map(reservations, entries) ;
						break;
				case template: reservations= copy(template, getBeginn());map(reservations, entries) ;break;
				case zu_loeschen: remove(reservations);break;
			}
		}


		private void remove(Reservation[] reservations) throws RaplaException 
		{
			getModification().removeObjects( reservations);
		}


		private Reservation[] copy(Template template2, Date beginn) throws RaplaException 
		{
			Reservation[] toCopy = template2.reservations;
		     List<Reservation> sortedReservations = new ArrayList<Reservation>( Arrays.asList( toCopy));
		     Collections.sort( sortedReservations, new ReservationStartComparator(getLocale()));
		     Collection<Reservation> copies = new ArrayList<Reservation>();
		     Date firstStart = null;
		     for (Reservation reservation: sortedReservations) {
		    	 if ( firstStart == null )
		    	 {
		    		 firstStart = ReservationStartComparator.getStart( reservation);
		    	 }
		    	 Reservation copy = copy(reservation, beginn, firstStart);
		    	 copies.add( copy);
		     }
		    
			return copies.toArray(Reservation.RESERVATION_ARRAY);
		}
		
		public Reservation copy(Reservation reservation, Date destStart,Date firstStart) throws RaplaException {
			Reservation r = (Reservation) getModification().clone( reservation);
	        
			Appointment[] appointments = r.getAppointments();
		
			for ( Appointment app :appointments) {
				Repeating repeating = app.getRepeating();
			    
			    Date oldStart = app.getStart();
			    // we need to calculate an offset so that the reservations will place themself relativ to the first reservation in the list
			    long offset = DateTools.countDays( firstStart, oldStart) * DateTools.MILLISECONDS_PER_DAY;
			    Date newStart ;
			    Date destWithOffset = new Date(destStart.getTime() + offset );
			    newStart = getRaplaLocale().toDate(  destWithOffset  , oldStart );
			    app.move( newStart) ;
			    if (repeating != null)
			    {
			    	Date[] exceptions = repeating.getExceptions();
		    		repeating.clearExceptions();
		       		for (Date exc: exceptions)
		    		{
		    		 	long days = DateTools.countDays(oldStart, exc);
		        		Date newDate = DateTools.addDays(newStart, days);
		        		repeating.addException( newDate);
		    		}
			    	
			    	if ( !repeating.isFixedNumber())
			    	{
			        	Date oldEnd = repeating.getEnd();
			        	if ( oldEnd != null)
			        	{
			        		// If we don't have and endig destination, just make the repeating to the original length
			        		long days = DateTools.countDays(oldStart, oldEnd);
			        		Date end = DateTools.addDays(newStart, days);
			        		repeating.setEnd( end);
			        	}
			    	}	    
			    }
			}
			return r;
		}


		private void map(Reservation[] reservations2,Map<String, Object> entries2) throws RaplaException 
		{
			ArrayList<Reservation> toStore = new ArrayList<Reservation>();
			for (Reservation r: reservations2)
			{
				Reservation edit;
				if ( r.isPersistant())
				{
					edit = getModification().edit(r);
				}
				else
				{
					edit = r;
				}
				map( edit.getClassification(), entries2);
				toStore.add( edit);
			}
			getModification().storeObjects( toStore.toArray( Reservation.RESERVATION_ARRAY));
		}


		private void map(Classification c, Map<String, Object> entries2) {
			for (Map.Entry<String, Object> e: entries.entrySet())
			{
				String key = e.getKey();
				Object value = e.getValue();
				Attribute attribute = c.getAttribute( key );
				if ( attribute != null && value!= null)
				{
					Object convertValue = attribute.convertValue(value );
					c.setValue( key, convertValue);
				}
			}
		}
		
		public boolean needsUpdate(Reservation[] events, Map<String, Object> entries2) {
			for ( Reservation r:events)
			{
				if ( needsUpdate( r.getClassification(), entries2))
				{
					return true;
				}
			}
			return false;
		}
		
		public boolean needsUpdate(Classification c, Map<String, Object> entries2) {
			for (Map.Entry<String, Object> e: entries.entrySet())
			{
				String key = e.getKey();
				Object value = e.getValue();
				Attribute attribute = c.getAttribute( key );
				if ( attribute != null && value!= null)
				{
					Object convertValue = attribute.convertValue(value );
					Object currentValue = c.getValue( key);
					if ( convertValue != null)
					{
						if ( currentValue == null)
						{
							return true;
						}
						else
						{
							if ( !convertValue.equals( currentValue))
							{
								return true;
							}
						}
					}
				}
			}
			return false;
		}
		
    }
    
	 private void confirmImport(final Component parentComponent, final String[] header, final List<Entry> entries) throws RaplaException {
         Object[][] tableContent = new Object[entries.size()][header.length+2];
         
         DynamicType[] dynamicTypes = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
         for (int i = 0; i < entries.size(); i++)
         { 	 
        	 Entry row = entries.get(i);
        	 {
        		 String primaryKey = (String) row.get(PRIMARY_KEY);
	        	 List<ClassificationFilter> filters = new ArrayList<ClassificationFilter>();
	        	 for (DynamicType type:dynamicTypes)
	        	 {
	 				if (type.getAttribute(PRIMARY_KEY) != null ) 
	 				{
	 					ClassificationFilter filter = type.newClassificationFilter();
	 					filter.addEqualsRule(PRIMARY_KEY, primaryKey);
	 					filters.add( filter);
	 				}
	        	 }
	        	 User user = null;
	        	 Date start=null;
	        	 Date end = null;
	         
	        	 Reservation[] reservations = getQuery().getReservations(user, start, end, filters.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY));
	        	 row.reservations = reservations;
        	 }
        	 if ( row.reservations.length == 0)
        	 {
            	 {
            		 String primaryKey = (String) row.get(TEMPLATE_KEY);
    	        	 List<ClassificationFilter> filters = new ArrayList<ClassificationFilter>();
    	        	 for (DynamicType type:dynamicTypes)
    	        	 {
    	        		 if (type.getAttribute(PRIMARY_KEY) != null && type.getAttribute(TEMPLATE_KEY) != null ) 
    	        		 {
    	        			 {
    	        				 ClassificationFilter filter = type.newClassificationFilter();
    	        				 filter.addEqualsRule(PRIMARY_KEY, "");
    	        				 filter.addEqualsRule(PRIMARY_KEY, null);
    	        				 filters.add( filter);
    	        			 }
    	        			 {
    	        				 ClassificationFilter filter = type.newClassificationFilter();
    	        				 filter.addEqualsRule(TEMPLATE_KEY, primaryKey);
    	        				 filters.add( filter);
    	        			 }
    	 				}
    	        	 }
    	        	 User user = null;
    	        	 Date start=null;
    	        	 Date end = null;
    	         
    	        	 Reservation[] reservations = getQuery().getReservations(user, start, end, filters.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY));
    	        	 if ( reservations.length >0 )
    	        	 {
    	        		 row.template = new Template(reservations);
    	        	 } 
            	 }
        	 }
    	 }
        	
         List<Template> templateList = new ArrayList<ImportTemplatePluginInitializer.Template>();
    	 {
        	 List<ClassificationFilter> filters = new ArrayList<ClassificationFilter>();
        	 for (DynamicType type:dynamicTypes)
        	 {
        		 if (type.getAttribute(PRIMARY_KEY) != null && type.getAttribute(TEMPLATE_KEY) != null ) 
        		 {
        			 {
        				 ClassificationFilter filter = type.newClassificationFilter();
        				 filter.addEqualsRule(PRIMARY_KEY, "");
        				 filter.addEqualsRule(PRIMARY_KEY, null);
        				 filters.add( filter);
        			 }
        			 {
        				 ClassificationFilter filter = type.newClassificationFilter();
        				 filter.addEqualsRule(TEMPLATE_KEY, "-1");
        				 filters.add( filter);
        			 }
 				}
        	 }
        	 User user = null;
        	 Date start=null;
        	 Date end = null;
         
        	 Reservation[] reservations = getQuery().getReservations(user, start, end, filters.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY));
        	 for (Reservation event:reservations)
        	 {
        		 Template template = new Template( new Reservation[] {event});
        		 templateList.add( template );
        	 }
    	 }
         for (int i = 0; i < entries.size(); i++)
         { 	 
        	 Entry row = entries.get(i);
        	 for (int j = 0; j < header.length ; j++) 
        	 {
        		 Object object = row.get(header[j]);
        		 if ( object != null)
        		 {
        			 tableContent[i][j] = object.toString();
        		 }
        	 }
    		 tableContent[i][header.length] = row.getStatus();
     		 tableContent[i][header.length+1] = row.template;
         }

         JTable table = new JTable();
         String[] newHeader = new String[header.length+2];
         System.arraycopy(header, 0, newHeader, 0, header.length);
         newHeader[header.length] = "status";
         newHeader[header.length+1] = "template";
         
         DefaultTableModel dataModel = new DefaultTableModel(tableContent, newHeader)
         {
        	@Override
        	public boolean isCellEditable(int row, int column) {
        		Entry entry = entries.get(row);
        		if ( column == header.length+1)
        		{
        			if ( entry.getStatus() != Status.template_waehlen )
        			{
        				return false;
        			}
        		}
        		return super.isCellEditable(row, column);
        	} 
         };
         table.setModel(dataModel);
         table.getColumnModel().getColumn( header.length).setMinWidth(120);
         {
        	 TableColumn templateColumn = table.getColumnModel().getColumn( header.length+1);
        	 templateColumn.setMinWidth(120);
        	 templateColumn.setCellEditor( new JIDCellEditor(templateList));
         }
         JScrollPane pane = new JScrollPane(table);
		 DialogUI dialog2 = DialogUI.create(
                 getContext()
                 ,parentComponent
                 ,true
                 ,pane
                 ,new String[] {getString("ok"),getString("back")}
        );
		 pane.setPreferredSize(new Dimension(900, 600));
         dialog2.setIcon(getIcon("icon.info"));
         dialog2.setDefault(0);
         dialog2.start();
         if (dialog2.getSelectedIndex() == 0)
         {
        	 for (int i=0;i<entries.size();i++)
        	 {
        		 Entry entry = entries.get(i);
        		 Template template = (Template)table.getValueAt(i, header.length+1);
        		 entry.template = template;
        		 try {
					entry.process();
				} catch (ParseException e) {
					throw new RaplaException( e);
				}
        	 }
         }
         return;
	}
   
}