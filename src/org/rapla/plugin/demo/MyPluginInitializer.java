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
package org.rapla.plugin.demo;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.rapla.components.iolayer.FileContent;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.examples.RaplaImportUsers;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.plugin.RaplaExtensionPoints;


public class MyPluginInitializer extends RaplaGUIComponent
{

    public MyPluginInitializer(RaplaContext sm) throws RaplaException {
        super(sm);
        MenuExtensionPoint helpMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.HELP_MENU_EXTENSION_POINT);
        helpMenu.insert(createInfoMenu() );
        
        MenuExtensionPoint importMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.IMPORT_MENU_EXTENSION_POINT);
        importMenu.insert( createImportMenu());

        MenuExtensionPoint export = (MenuExtensionPoint) getService( RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT);
        export.insert(createExportMenu() );
    }

    private JMenuItem createInfoMenu( ) {
        JMenuItem item = new JMenuItem( "my UseCase" );
        item.setIcon( getIcon("icon.help") );
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        final MyDialog myDialog = new MyDialog(getContext());
                        DialogUI dialog = DialogUI.create( getContext(),getMainComponent(),true, myDialog.getComponent(), new String[] {getString("ok")});
                        dialog.setTitle( "My Usecase");
                        dialog.setSize( 800, 600);
                        dialog.startNoPack();
                     } catch (Exception ex) {
                        showException( ex, getMainComponent() );
                    }
                }
        });
        return item;
    }

    private JMenuItem createImportMenu( ) {
        JMenuItem item = new JMenuItem( "Users from CSV" );
        item.setIcon( getIcon("icon.import") );
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
                        IOInterface io = (IOInterface) getService( IOInterface.class);
                        FileContent file = io.openFile( frame, null, new String[] {".csv"});
                        if ( file != null) {
                            Reader reader = new InputStreamReader( file.getInputStream());
                            RaplaImportUsers.importUsers( getClientFacade(), reader);
                        }
                     } catch (Exception ex) {
                        showException( ex, getMainComponent() );
                    }
                }
        });
        return item;
    }

    private JMenuItem createExportMenu( )  {
        JMenuItem item = new JMenuItem( "dummy export" );
        item.setIcon( getIcon("icon.export") );
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        CalendarModel model = getService(CalendarModel.class);
                        export( model, getMainComponent());
                    } catch (Exception ex) {
                        showException( ex, getMainComponent() );
                    }
                }
        });
        return item;
    }
    
    
    public void export(final CalendarModel model,final Component parentComponent) throws Exception
    {
        final Reservation[] events = model.getReservations();
        // generates a text file from all filtered events;
        StringBuffer buf = new StringBuffer();
        buf.append( "List of all events:");
        buf.append('\n');
        for (int i=0;i< events.length;i++)
        {
            buf.append(getClassificationLine(events[i].getClassification()));
            buf.append('\n');
        }
        saveFile( buf.toString().getBytes(), "list.txt","txt");
    }

    private String getClassificationLine( Classification classification) {
        Attribute[] attributes = classification.getAttributes();
        StringBuffer buf = new StringBuffer();
        for ( int i= 0;i < attributes.length; i++ )
        {
            Object value = classification.getValue( attributes[i]);
            if ( value != null )
            {
                buf.append( getName( value) );
            }
            buf.append(';');
        }
        return buf.toString();
    }

    public void saveFile(byte[] content,String filename, String extension) throws RaplaException {
        final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
        IOInterface io = (IOInterface) getService( IOInterface.class);
        try {
            io.saveFile( frame, null, new String[] {extension}, filename, content);
        } catch (IOException e) {
            throw new RaplaException("Cant export file!", e);
        }
    }




}

