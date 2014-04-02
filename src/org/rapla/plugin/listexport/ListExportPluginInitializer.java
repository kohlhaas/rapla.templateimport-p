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
package org.rapla.plugin.listexport;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.rapla.plugin.dbexport.DBExportOption;
import org.rapla.plugin.dbexport.RaplaDBExport;

public class ListExportPluginInitializer extends RaplaGUIComponent implements IdentifiableMenuEntry, ActionListener
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
	boolean webstartEnabled = false;
	//Category superCategory = getQuery().getSuperCategory();
	ClassificationFilter[] exportCriteria;
    CalendarModel model = getContext().lookup(CalendarModel.class);

    JMenuItem item;
    String id = "list";
    
	public ListExportPluginInitializer(RaplaContext sm) throws RaplaException {
        super(sm);
        setChildBundleName( ListExportPlugin.RESOURCE_FILE);
        webstartEnabled =getContext().lookup(StartupEnvironment.class).getStartupMode() == StartupEnvironment.WEBSTART;   

		item = new JMenuItem( getString(id));
        item.setIcon( getIcon("icon.list") );
        item.addActionListener(this);
    }
	
	public String getId() {
		return id;
	}
	
	public JMenuItem getMenuElement() {
		return item;
	}
	
	public void actionPerformed(ActionEvent evt) {
        try {
        	//if(isSelectionOK())
            	export( model, getMainComponent());
        } catch (Exception ex) {
            showException( ex, getMainComponent() );
        }
    }
    
    public void export(final CalendarModel model,final Component parentComponent) throws IOException,  RaplaException
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
    	
    	// Pass request for treatment
    	Collection<RaplaObject> selection = model.getSelectedObjects();

    	StringBuffer out = new StringBuffer();
    	int exportCnt= 0;
    	for(Iterator<RaplaObject> it = selection.iterator();it.hasNext();)
        {
    		String tableName = null;
        	Object obj = it.next();
        	if ( obj instanceof DynamicType ) {
        		// export Meta data
        		tableName = ((DynamicType) obj).getName(locale);
        		attributesMetaData((DynamicType) obj,out);
        		exportCnt++;
        	    Allocatable[]  alloc = model.getSelectedAllocatables();
        	    for ( int i=0;i< alloc.length;i++)
        	    {
        	    	// export data
        	    	allocatableData((DynamicType) obj, alloc[i], out); 
        	    	out.append("\n");
        	    	exportCnt++;
        	    }
        	    saveFile( out.toString(), tableName + ".csv","csv",parentComponent);    
        	}
        	if(exportCnt == 0) {
        		DialogUI dialog2 = DialogUI.create(
                        getContext()
                        ,parentComponent
                        ,true
                        ,getString("error")
                        ,getString("no_selection")
                        ,new String[] {getString("back")}
               );
                dialog2.setIcon(getIcon("icon.error"));
                dialog2.setDefault(0);
                dialog2.start();
        		return;
        	}        	
        }	
    }
    
	public String getColumnName (String name) {
    	return (QUOTEBEFORE + name + QUOTEAFTER);
	}

	public void saveFile(String content,String filename, String extension, final Component parentComponent) throws RaplaException {
		final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
		IOInterface io =  getService( IOInterface.class);
		String path = null;
		try {
			path = io.saveFile( frame, null, new String[] {extension}, filename, content.getBytes());
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
     
	private void allocatableData(DynamicType obj, Allocatable allc, StringBuffer out) {
		
		Classification classification = allc.getClassification();
		if(classification.getType() != obj)
			return;
		// Resource_Id
		String allocatableId = RaplaDBExport.getShortId( allc);
		out.append(allocatableId);
				
		Attribute[] attributes = classification.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			String attributeKey = attributes[k].getKey();
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
				Category cat = (Category) classification.getValue(attributeKey);  
				if ( cat != null ) {
// BJO 00000091 
		            Category rootCategory = (Category) attributes[k].getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
		            out.append(SEPARATOR + QUOTEBEFORE + cat.getPath(rootCategory, locale).replace("/", " ") + QUOTEAFTER);
// BJO 00000091
					//out.append(SEPARATOR + QUOTEBEFORE + cat.getPath(superCategory, locale).replace("/", " ") + QUOTEAFTER);
				}
				else {
					out.append(SEPARATOR + QUOTEBEFORE + QUOTEAFTER);
				}
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
							out.append(SEPARATOR + QUOTEBEFORE + attributeValue.toString().replace("&", "") + QUOTEAFTER);
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
		out.append(SEPARATOR + QUOTEBEFORE + ((Allocatable) allc).getOwner().getName() + QUOTEAFTER);
		if(((Allocatable) allc).getCreateTime() != null)
			out.append(SEPARATOR + QUOTEBEFORE + sdfdatetime.format( ((Allocatable) allc).getCreateTime() ) + QUOTEAFTER);
		else
			out.append(SEPARATOR);
		if(((Allocatable) allc).getLastChangedBy() == null)
			out.append(SEPARATOR + QUOTEBEFORE + ((Allocatable) allc).getOwner().getName() + QUOTEAFTER);
		else
			out.append(SEPARATOR + QUOTEBEFORE + ((Allocatable) allc).getLastChangedBy().getName() + QUOTEAFTER);
		if(((Allocatable) allc).getLastChangeTime() != null)
			out.append(SEPARATOR + QUOTEBEFORE + sdfdatetime.format( ((Allocatable) allc).getLastChangeTime() ) + QUOTEAFTER);
		else
			out.append(SEPARATOR);
	}
	
	private void attributesMetaData(DynamicType obj, StringBuffer out) {
		
		String tableName = obj.getName(locale);
		out.append(getColumnName (tableName + "_" + getString("Id")));
		Attribute[] attributes = obj.getAttributes();
		for (int k=0; k<attributes.length; k++)
		{      
			//String attributeKey = attributes[k].getKey();
			String attributeName = attributes[k].getName(locale);
			
			if (attributes[k].getType() == AttributeType.CATEGORY) {
					out.append(SEPARATOR + getColumnName (tableName + " " + attributeName));
			}
			else 
				if (attributes[k].getType() == AttributeType.DATE) {
					out.append(SEPARATOR + getColumnName (tableName + " " + attributeName) );
				}						
				else 
					if (attributes[k].getType() == AttributeType.STRING) {
						out.append(SEPARATOR + getColumnName (tableName + " " + attributeName) );
					}
					else 
						if (attributes[k].getType() == AttributeType.INT) {
							out.append(SEPARATOR + getColumnName (tableName + " " + attributeName) );
						}
						else 
							if (attributes[k].getType() == AttributeType.BOOLEAN) {
								out.append(SEPARATOR + getColumnName (tableName + " " + attributeName) );
							}
		}
		out.append(SEPARATOR + getColumnName (getString("createdby")) );
		out.append(SEPARATOR + getColumnName (getString("createdat")) ); 
		out.append(SEPARATOR + getColumnName (getString("changedby")) );
		out.append(SEPARATOR + getColumnName (getString("changedat")) );
    	out.append("\n"); 
	}

	private boolean isSelectionOK() throws RaplaException {
		exportCriteria = ((CalendarSelectionModel) model).getReservationFilter();
	    boolean selectionOK = true;
    	if( !((CalendarSelectionModel)model).getViewId( ).equals("table"))
    		selectionOK = false;
    	/*
    	else
    		if( exportCriteria.length == 0)
    			selectionOK = false;
    			*/
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