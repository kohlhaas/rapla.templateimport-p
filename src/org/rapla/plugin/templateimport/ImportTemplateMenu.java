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
import java.awt.BorderLayout;
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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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
import org.rapla.entities.User;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationAnnotations;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.rapla.gui.toolkit.RaplaButton;
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
    
	 public class JIDCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

		private static final long serialVersionUID = 1L;
		JComboBox jComboBox;
	    Collection<String> templates;

	    JIDCellEditor( Collection<String> templates)
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
	        jComboBox.addActionListener( this);
	        return jComboBox;
	    }

		public void actionPerformed(ActionEvent e) {
			fireEditingStopped();
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

    class Entry
    {
    	private Map<String, Object> entries;
    	private List<Reservation> reservations = new ArrayList<Reservation>();
		private String template;
		
		
    	public Entry(Map<String, Object> entries) {
    		this.entries = entries;
    	}

    	public void setReservations( List<Reservation> events)
    	{
    	    this.reservations = events;
    	}
    	
    	public Date getBeginn() throws Exception
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
    		        SerializableDateTimeFormat format = getRaplaLocale().getSerializableFormat();
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
            String string = (String)entries.get(TemplateImport.STORNO_KEY);
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
			} catch (Exception e) {
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

		public void process() throws Exception {
			Status status =getStatus();
			switch (status )
			{
				case geloescht: break;
				case aktuallisieren: map(reservations, entries) ;
						break;
				case template_waehlen: 
					if ( template != null )
					{
						Collection<Reservation> templateReservations = getTemplateReservations();
						reservations= copy(templateReservations, getBeginn());
						map(reservations, entries) ;break;
					}
					break;
				case template: 
					Collection<Reservation> templateReservations = getTemplateReservations();
					reservations= copy(templateReservations, getBeginn());
					map(reservations, entries) ;break;
				case zu_loeschen: remove(reservations);break;
			}
		}

		protected Collection<Reservation> getTemplateReservations() throws RaplaException {
			if ( template == null)
			{
				return Collections.emptyList();
			}
			Collection<Reservation> reservations = getQuery().getTemplateReservations(template);
			return reservations;
		}

		private void remove(List<Reservation> reservations) throws RaplaException 
		{
			getModification().removeObjects( reservations.toArray( Reservation.RESERVATION_ARRAY));
		}

		private void map(List<Reservation> reservations,Map<String, Object> entries) throws RaplaException 
		{
			ArrayList<Reservation> toStore = new ArrayList<Reservation>();
			Collection<Reservation> editObjects = getModification().edit(reservations);
			for (Reservation reservation: editObjects)
			{
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
				if ( key.equals( TemplateImport.PRIMARY_KEY) && value != null)
				{
					reservation.setAnnotation(ReservationAnnotations.KEY_EXTERNALID, value.toString());
				}
				Attribute attribute = c.getAttribute( key );
				if ( attribute != null && value!= null)
				{
					Object convertValue = attribute.convertValue(value );
					c.setValue( key, convertValue);
				}
			}
			if ( reservation.getAnnotation( ReservationAnnotations.KEY_EXTERNALID) == null)
			{
				throw new RaplaException("Primary Key [" + TemplateImport.PRIMARY_KEY + "] not set in row " + entries.toString()  );
			}
		}
		
		private boolean needsUpdate(List<Reservation> events, Map<String, Object> entries) {
			for ( Reservation r:events)
			{
				if ( needsUpdate( r.getClassification(), entries))
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

        public List<Reservation> getReservations() {
            return reservations;
        }
    }
    
	 private void confirmImport(final Component parentComponent, final String[] header, final List<Entry> entries) throws RaplaException {
     
		 Object[][] tableContent = new Object[entries.size()][header.length + 3];
         
         Map<String, List<Reservation>> keyMap = getImportedReservations();
         Collection<String> templateMap = getQuery().getTemplateNames();
         for (int i = 0; i < entries.size(); i++)
         { 	 
        	 Entry row = entries.get(i);
        	 {
        		 String primaryKey = (String) row.get(TemplateImport.PRIMARY_KEY);
        		 List<Reservation> events = keyMap.get( primaryKey);
        		 if ( events != null)
        		 {
        		     row.setReservations(events);
        		 }
        	 }
        	 if ( row.getReservations().size() == 0)
        	 {
        		 String key = (String) row.get(TemplateImport.TEMPLATE_KEY);
                  
        		 if (  templateMap.contains( key ))
	        	 {
	        		 row.template = key;
	        	 } 
        	 }
    	 }
         final int selectCol = 0;
         final int statusCol = header.length+1;	
         final int templateCol = header.length+2;	
         for (int i = 0; i < entries.size(); i++)
         { 	 
        	 Entry row = entries.get(i);
        	 
        	 for (int j = 0; j < header.length ; j++) 
        	 {
        		 Object object = row.get(header[j]);
        		 if ( object != null)
        		 {
        			 tableContent[i][j+1] = object.toString();
        		 }
        	 }
    		 Status status = row.getStatus();
    		 tableContent[i][statusCol] = status;
     		 tableContent[i][templateCol] = row.template;
     		 tableContent[i][selectCol] = status != Status.aktuell && status != Status.template_waehlen && status != Status.template;
         }

         final JTable table = new JTable();
     	 table.putClientProperty("terminateEditOnFocusLost", true);
         ActionListener copyListener = new ActionListener() {
     		
     		public void actionPerformed(ActionEvent evt) 
     		{
     	        copy(table, evt);            
     		}

     	};
     	table.registerKeyboardAction(copyListener,getString("copy"),COPY_STROKE,JComponent.WHEN_FOCUSED);
         String[] newHeader = new String[header.length+3];
         newHeader[selectCol] = "";
         System.arraycopy(header, 0, newHeader, 1, header.length);
         newHeader[statusCol] = "status";
         newHeader[templateCol] = "template";
         final DefaultTableModel dataModel = new DefaultTableModel(tableContent, newHeader)
         {
        	@Override
        	public boolean isCellEditable(int row, int column) {
        		Entry entry = entries.get(row);
        		if ( column == templateCol)
        		{
        			Status status = entry.getStatus();
					if ( status != Status.template_waehlen && status != Status.template)
        			{
        				return false;
        			}
        		}
        		if ( column == selectCol)
        		{
        			return true;
        		}
        		return super.isCellEditable(row, column);
        	} 
        	
        	@Override
        	public Class<?> getColumnClass(int columnIndex) {
        		if ( columnIndex == selectCol)
        		{
        			return Boolean.class;
        		}
        		return super.getColumnClass(columnIndex);
        	}
         };
         table.setModel(dataModel);
         table.getColumnModel().getColumn( selectCol ).setWidth(15);
         table.getColumnModel().getColumn( selectCol ).setMaxWidth(20);
//         JCheckBox checkBoxC = new JCheckBox();
//		TableCellEditor checkBox = new DefaultCellEditor(checkBoxC);
//		DefaultTableCellRenderer.
         table.getColumnModel().getColumn( statusCol ).setMinWidth(100);
         {
        	 TableColumn templateColumn = table.getColumnModel().getColumn( templateCol);
        	 templateColumn.setMinWidth(250);
        	 Collection<String> sortedTemplates = new TreeSet<String>(templateMap);
        	 templateColumn.setCellEditor( new JIDCellEditor(sortedTemplates));
         }
         final RaplaButton everythingButton = new RaplaButton(RaplaButton.SMALL);
         final RaplaButton  nothingButton = new RaplaButton(RaplaButton.SMALL);
         
         everythingButton.setText( getString("select_everything") );
         everythingButton.setIcon( getIcon("icon.all-checked"));
         nothingButton.setText(getString("select_nothing"));
         nothingButton.setIcon( getIcon("icon.all-unchecked"));
         
         JPanel buttonPanel = new JPanel();
         buttonPanel.add( everythingButton );
         buttonPanel.add( nothingButton );
         
         JScrollPane pane = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
       
         JPanel content = new JPanel();
         content.setLayout( new BorderLayout());
         content.add( buttonPanel, BorderLayout.NORTH);
         content.add( pane, BorderLayout.CENTER);
         ActionListener listener = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				boolean set = source == everythingButton;
				int rowCount = dataModel.getRowCount();
				for ( int row=0;row<rowCount;row++)
				{
					dataModel.setValueAt(new Boolean(set),row, selectCol );
				}
			}
		};
		everythingButton.addActionListener( listener);
		nothingButton.addActionListener(listener);
         //pane.setPreferredSize( new Dimension(2000,800));
		 DialogUI dialog2 = DialogUI.create(
                 getContext()
                 ,parentComponent
                 ,true
                 ,content
                 ,new String[] {getString("ok"),getString("back")}
        );
		 pane.setPreferredSize(new Dimension(1024, 700));
         dialog2.setDefault(0);
         dialog2.start();

         if ( dialog2.getSelectedIndex() == 0)
         {
		     try
		     {
		    	for (int i=0;i<entries.size();i++)
		    	{
		    		 Entry entry = entries.get(i);
		    		 Boolean selected = (Boolean)table.getValueAt(i, selectCol);
		    		 if ( selected == null || !selected)
		    		 {
		    			 continue;
		    		 }
		    		 String template = (String)table.getValueAt(i, templateCol);
		    		 if ( template != null)
		    		 {
		    			 entry.template = template;
		    		 }
		    		 entry.process();
				}		// TODO Auto-generated method stub
			} catch (Exception ex) {
			//	showException(ex,null);
				throw new RaplaException(ex.getMessage(), ex);
			}
     	 }
 
//         dialog2.getButton( 0 ).addActionListener( new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				SwingUtilities.invokeLater( new Runnable() {
//					
//					@Override
//					public void run() {
//						try {
//						
//					}
//				}); 
//							}
//		});
         return;
	}




	protected Map<String, List<Reservation>> getImportedReservations()
			throws RaplaException {
		User user = null;
         Date start=getQuery().today();
         Date end = null;
         Map<String,List<Reservation>> keyMap = new LinkedHashMap<String, List<Reservation>>();

         Reservation[] reservations = getQuery().getReservations(user, start, end, null);
         
         for ( Reservation r:reservations)
         {
             String key = r.getAnnotation(ReservationAnnotations.KEY_EXTERNALID);
             if  ( key != null )
             {
                 List<Reservation> list = keyMap.get( key);
                 if ( list == null)
                 {
                     list = new ArrayList<Reservation>();
                     keyMap.put( key, list);
                 }
                 list.add( r);
             }
         }
		return keyMap;
	}
   
}