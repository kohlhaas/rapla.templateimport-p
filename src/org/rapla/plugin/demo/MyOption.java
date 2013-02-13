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

import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.rapla.entities.configuration.Preferences;
import org.rapla.gui.OptionPanel;

/** sample Option that only displays a JCheckBox */
public class MyOption implements OptionPanel {
    Preferences preferences;
    String MY_OPTION = "org.rapla.plugin.demo.MyOption";
    JCheckBox checkBox = new JCheckBox("check or uncheck");
    public MyOption() {
    }
    public JComponent getComponent() {
        return checkBox;
    }
    public String getName(Locale locale) {
        return "My Option";
    }

    public void show() {
        checkBox.setSelected( preferences.getEntryAsBoolean(MY_OPTION, false) );
    }

    public void commit() {
        preferences.putEntry(MY_OPTION, String.valueOf( checkBox.isSelected()));
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

}
