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
package org.rapla.plugin.dbexport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

public class DBExportOption extends RaplaGUIComponent implements OptionPanel {
	
	public final static String decimalpoint_CONFIG = "org.rapla.plugin.DBExportOption.decimalpoint";	
	public final static String DECIMAL_POINT = ".";
	public final static String DECIMAL_COMMA = ",";
	
	public final static String separator_CONFIG = "org.rapla.plugin.DBExportOption.separator";
	public final static String SEPARATOR_SPACE = " ";
	public final static String SEPARATOR_SEMICOLON = ";";
	public final static String SEPARATOR_COMMA = ",";
	public final static String SEPARATOR_PIPE = "|";
	public final static String SEPARATOR_COLON = ":";
	
	public final static String dateformat_CONFIG = "org.rapla.plugin.DBExportOption.dateformat";
	public final static String FORMAT_YYYYMMDD = "yyyy-MM-dd";
	public final static String FORMAT_DDMMYYYY = "dd-MM-yyyy";

	public final static String datetimeformat_CONFIG = "org.rapla.plugin.DBExportOption.datetimeformat";
	public final static String FORMAT_YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";
	public final static String FORMAT_DDMMYYYHHMMSS = "dd-MM-yyyy HH:mm:ss";

	public final static String quote_CONFIG = "org.rapla.plugin.DBExportOption.quote";
	public final static String QUOTE_NONE = "";
	public final static String QUOTE_APOST = "'";
	public final static String QUOTE_DOUBLE = "\"";
	
	public final static String removeChars_CONFIG = "org.rapla.plugin.DBExportOption.removechars";
		
    Preferences preferences;
    String DBExport = "org.rapla.plugin.dbexport.DBExport";
    JPanel panel = new JPanel();
    
    JComboBox separator = new JComboBox( new String[] {SEPARATOR_SEMICOLON, SEPARATOR_SPACE, SEPARATOR_COMMA, SEPARATOR_COLON, SEPARATOR_PIPE});
    JComboBox decimalpoint = new JComboBox( new String[] {DECIMAL_POINT, DECIMAL_COMMA});
    JComboBox dateformat = new JComboBox( new String[] {FORMAT_YYYYMMDD, FORMAT_DDMMYYYY});
    JComboBox datetimeformat = new JComboBox( new String[] {FORMAT_YYYYMMDDHHMMSS, FORMAT_DDMMYYYHHMMSS});
    JComboBox quote = new JComboBox( new String[] {QUOTE_NONE, QUOTE_APOST, QUOTE_DOUBLE});
    JTextField removeChars = new JTextField();
    
    Boolean listenersEnabled = false;
    
    public DBExportOption(RaplaContext sm) throws RaplaException {
            super( sm);
            setChildBundleName( DBExportPlugin.RESOURCE_FILE);
        }

    public void create() throws RaplaException {
    	
        double pre = TableLayout.PREFERRED;
        double fill = TableLayout.FILL;
        //  columns = 2, rows = 5
        panel.setLayout( new TableLayout(new double[][] {{pre,5 ,pre,5}, {pre,5, pre,5, pre,5, pre,5, pre,5 , pre,5, fill}}));
        
        Listener listener = new Listener();
        panel.add( new JLabel(getString("separator")),"0,0");
        panel.add( separator,"2,0");
        separator.addActionListener( listener);
        
        panel.add( new JLabel(getString("decimalpoint")),"0,2");
        panel.add( decimalpoint,"2,2");
        decimalpoint.addActionListener( listener);
        
        panel.add( new JLabel(getString("dateformat")),"0,4");
        panel.add( dateformat,"2,4");
        
        panel.add( new JLabel(getString("datetimeformat")),"0,6");
        panel.add( datetimeformat,"2,6");
        
        panel.add( new JLabel(getString("quote")),"0,8");
        panel.add( quote,"2,8");
        quote.addActionListener( listener);

        panel.add( new JLabel(getString("removechars")),"0,10");
        panel.add( removeChars,"2,10");
    }
    
    public JComponent getComponent() {
        return panel;
    }
    public String getName(Locale locale) {
        return "Export";
    }

    public void show() throws RaplaException {
    	listenersEnabled = false;
        separator.setSelectedItem( preferences.getEntryAsString(separator_CONFIG, SEPARATOR_SEMICOLON) );
        decimalpoint.setSelectedItem( preferences.getEntryAsString(decimalpoint_CONFIG, DECIMAL_POINT) );
        dateformat.setSelectedItem(  preferences.getEntryAsString(dateformat_CONFIG, FORMAT_YYYYMMDD) );
        datetimeformat.setSelectedItem(  preferences.getEntryAsString(datetimeformat_CONFIG, FORMAT_YYYYMMDDHHMMSS) );
        String quoteChar = preferences.getEntryAsString(quote_CONFIG, QUOTE_NONE);
        if(quoteChar.length()==0)
        	quote.setSelectedIndex(0);
        else
        	quote.setSelectedItem( quoteChar );
        removeChars.setText(preferences.getEntryAsString(removeChars_CONFIG, SEPARATOR_SEMICOLON));
        create();
        listenersEnabled = true;
    }

    public void commit() {
        preferences.putEntry(separator_CONFIG, (String) separator.getSelectedItem());
        preferences.putEntry(decimalpoint_CONFIG, (String) decimalpoint.getSelectedItem());
        preferences.putEntry(dateformat_CONFIG, (String) dateformat.getSelectedItem());
        preferences.putEntry(datetimeformat_CONFIG, (String) datetimeformat.getSelectedItem());
        if(quote.getSelectedIndex()==0)
        	preferences.putEntry(quote_CONFIG, "");
        else
        	preferences.putEntry(quote_CONFIG, (String) quote.getSelectedItem());
        preferences.putEntry(removeChars_CONFIG, (String) removeChars.getText());
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    class Listener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
        	if(listenersEnabled == false)
        		return;
        	
            if ( evt.getSource() == separator) {
                ;
            }

            if ( evt.getSource() == decimalpoint) {
                ;
            }

            if ( evt.getSource() == quote) {
                ;
            }
        }
    }
}
