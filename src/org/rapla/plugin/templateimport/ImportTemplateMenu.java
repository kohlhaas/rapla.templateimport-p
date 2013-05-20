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
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.entities.Entity;
import org.rapla.entities.User;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.Template;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;


public class ImportTemplateMenu extends RaplaGUIComponent implements IdentifiableMenuEntry,ActionListener
{
	String id = "events into templates";
	JMenuItem item; 
	public ImportTemplateMenu(RaplaContext sm)  {
        super(sm);
       
        setChildBundleName( ImportTemplatePlugin.RESOURCE_FILE);
        
		item = new JMenuItem( id );
        item.setIcon( getIcon("icon.import") );
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
             Reader reader = null;
             TemplateImport importService = null;
             try
             {
                 importService = getWebservice( TemplateImport.class);
             }
             catch (Exception ex)
             {
                 
             }
             final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
             if ( importService != null && importService.hasDBConnection())
             {
                 reader = new StringReader( importService.importFromServer());
             }
             else
             {
                 IOInterface io =  getService( IOInterface.class);
                 FileContent file = io.openFile( frame, null, new String[] {".csv"});
                 if ( file != null) 
                 {
                     reader = new InputStreamReader( file.getInputStream());
                     //String[][] entries = Tools.csvRead( reader, ',',1 ); // read first 5 colums per input row and store in memory
                 }
                 
             }
             
             if ( reader != null)
             {
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
                          Entry e = new Entry(customerMap);
	                      Date beginn = e.getBeginn();
	                      if ( beginn != null && beginn.after( getQuery().today()))
	                      {
	                    	  list.add(e);
	                      }
                     }
                     confirmImport(frame, header, list);
                 }
                 finally {
                     if( mapReader != null ) {
                         mapReader.close();
                     }
                 }
             }
             
          } catch (Exception ex) {
             showException( ex, getMainComponent() );
         }
    }
    
	 public class JIDCellEditor extends AbstractCellEditor implements TableCellEditor {

		private static final long serialVersionUID = 1L;
		JComboBox jComboBox;
	    Collection<Template> templates;

	    JIDCellEditor( Collection<Template> templates)
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
    
    enum Status
    {
    	zu_loeschen,
    	aktuallisieren,
    	template,
    	template_waehlen,
    	geloescht, 
    	datum_fehlt, datum_fehlerhaft, aktuell
    }

    private static final String PRIMARY_KEY = "Seminarnummer";
    private static final String TEMPLATE_KEY = "TitelName";
    private static final String STORNO_KEY = "StorniertAm";
    
    class Entry
    {
    	private Map<String, Object> entries;
    	private List<Entity<Reservation>> reservations = new ArrayList<Entity<Reservation>>();
		private Template template;
		
		
    	public Entry(Map<String, Object> entries) {
    		this.entries = entries;
    	}

    	public void setReservations( List<Entity<Reservation>> events)
    	{
    	    this.reservations = events;
    	}
    	
    	public Date getBeginn() throws ParseException
    	{
            String dateString = (String)entries.get(TemplateImport.BEGIN_KEY);
    		if (dateString != null)
    		{
    		    Date parse;
    		    try
    		    {
        		    SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        			format.setTimeZone( DateTools.getTimeZone());
        			parse = format.parse( dateString);
    		    }
    		    catch (ParseException ex)
    		    {
    		        SerializableDateTimeFormat format = new SerializableDateTimeFormat( getRaplaLocale().createCalendar());
                    boolean fillDate = false;
    		        parse = format.parseDate( dateString, fillDate);
    		    }
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
            String string = (String)entries.get(STORNO_KEY);
       		if ( string != null && string.trim().length() > 0)
       		{
       			return true;
       		}
       		return false;
       	}

		public Status getStatus() 
		{
			boolean hasReservations = reservations != null && reservations.size() >0;
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
				case geloescht: break;
				case aktuallisieren: map(reservations, entries) ;
						break;
				case template: reservations= copy(template.getReservations(), getBeginn());map(reservations, entries) ;break;
				case zu_loeschen: remove(reservations);break;
			}
		}

		private void remove(List<Entity<Reservation>> reservations) throws RaplaException 
		{
			getModification().removeObjects( reservations.toArray( Reservation.RESERVATION_ARRAY));
		}


		

		private void map(List<Entity<Reservation>> reservations,Map<String, Object> entries) throws RaplaException 
		{
			ArrayList<Reservation> toStore = new ArrayList<Reservation>();
			Collection<Entity<Reservation>> editObjects = getModification().edit(reservations);
			for (Entity<Reservation> edit: editObjects)
			{
				Reservation reservation = edit.cast();
				map( reservation, entries);
				toStore.add( reservation);
			}
			getModification().storeObjects( toStore.toArray( Reservation.RESERVATION_ARRAY));
		}


		private void map(Reservation reservation,Map<String, Object> entries) throws RaplaException {
			Classification c = reservation.getClassification();
			for (Map.Entry<String, Object> e: entries.entrySet())
			{
				String key = e.getKey();
				Object value = e.getValue();
				if ( key.equals( PRIMARY_KEY) && value != null)
				{
					reservation.setAnnotation(Reservation.EXTERNALID, value.toString());
				}
				Attribute attribute = c.getAttribute( key );
				if ( attribute != null && value!= null)
				{
					Object convertValue = attribute.convertValue(value );
					c.setValue( key, convertValue);
				}
			}
			if ( reservation.getAnnotation( Reservation.EXTERNALID) == null)
			{
				throw new RaplaException("Primary Key [" + PRIMARY_KEY + "] not set in row " + entries.toString()  );
			}
		}
		
		private boolean needsUpdate(List<Entity<Reservation>> events, Map<String, Object> entries) {
			for ( Entity<Reservation> r:events)
			{
				if ( needsUpdate( r.cast().getClassification(), entries))
				{
					return true;
				}
			}
			return false;
		}
		
		private boolean needsUpdate(Classification c, Map<String, Object> entries) {
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

        public List<Entity<Reservation>> getReservations() {
            return reservations;
        }
		
    }
    
	 private void confirmImport(final Component parentComponent, final String[] header, final List<Entry> entries) throws RaplaException {
         Object[][] tableContent = new Object[entries.size()][header.length+2];
         
         
         User user = null;
         Date start=getQuery().today();
         Date end = null;
         Map<String,List<Entity<Reservation>>> keyMap = new LinkedHashMap<String, List<Entity<Reservation>>>();

         Reservation[] reservations = getQuery().getReservations(user, start, end, null);
         
         for ( Reservation r:reservations)
         {
             String key = r.getAnnotation(Reservation.EXTERNALID);
             if  ( key != null )
             {
                 List<Entity<Reservation>> list = keyMap.get( key);
                 if ( list == null)
                 {
                     list = new ArrayList<Entity<Reservation>>();
                     keyMap.put( key, list);
                 }
                 list.add( r);
             }
         }
         Map<String,Template> templateMap = getQuery().getTemplateMap();
         for (int i = 0; i < entries.size(); i++)
         { 	 
        	 Entry row = entries.get(i);
        	 {
        		 String primaryKey = (String) row.get(PRIMARY_KEY);
        		 List<Entity<Reservation>> events = keyMap.get( primaryKey);
        		 if ( events != null)
        		 {
        		     row.setReservations(events);
        		 }
        	 }
        	 if ( row.getReservations().size() == 0)
        	 {
        		 String key = (String) row.get(TEMPLATE_KEY);
        		 Template template = templateMap.get( key);
                  
        		 if ( template != null)
	        	 {
	        		 row.template = template;
	        	 } 
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

         final JTable table = new JTable();
     	
         ActionListener copyListener = new ActionListener() {
     		
     		public void actionPerformed(ActionEvent evt) 
     		{
     	        copy(table, evt);            
     		}

     	};
     	table.registerKeyboardAction(copyListener,getString("copy"),COPY_STROKE,JComponent.WHEN_FOCUSED);
        
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
         table.getColumnModel().getColumn( header.length).setMinWidth(100);
         {
        	 TableColumn templateColumn = table.getColumnModel().getColumn( header.length+1);
        	 templateColumn.setMinWidth(250);
        	 templateColumn.setCellEditor( new JIDCellEditor(templateMap.values()));
         }
         JScrollPane pane = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
         //pane.setPreferredSize( new Dimension(2000,800));
		 DialogUI dialog2 = DialogUI.create(
                 getContext()
                 ,parentComponent
                 ,true
                 ,pane
                 ,new String[] {getString("ok"),getString("back")}
        );
		 pane.setPreferredSize(new Dimension(1024, 700));
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